# iNXT BrokerVerse - BDOI Accounting, Disbursement and ACSL (BRD-5) Build Design

Status: **proposal for review**. It extends `docs/architecture/BROKING_ARCHITECTURE.md`,
`docs/architecture/OPERATIONS_DESIGN.md` and the Developer Guide, and does not change them. Requirements baseline:
[`BDOI_ACCT_BRD_SPEC.md`](../requirements/BDOI_ACCT_BRD_SPEC.md) (277 requirement rows, questions AQ01-AQ31). Every
class, migration and screen cites its BRD ID in Javadoc or a comment, for example `DIS 2.7.6`.

## 1. Design principles

1. **Extend the finance platform; do not build a second GL.** The chart, journals, accounting engine, open-item
   sub-ledger, periods, FX revaluation, year-end, bank reconciliation, payables masters and BIR returns already exist.
   FRBS requirements are delivered as changes to those modules (section 12.1). Only work that has no home becomes a new
   module: `disbursement`, `payrequest`, `acsl`, `frbs`.
2. **Disbursement sits behind the Operations port.** `opsledger.service.port.DisbursementGateway` stays the only way a
   business module asks for a payment. The in-app queue (`QueueDisbursementGateway`) is replaced by the `disbursement`
   adapter; remittance, cashiering, commission, `payrequest` and `frbs` do not know which one runs (OQ02).
3. **GL accounts are never chosen in code.** Every posting is a `BusinessEvent` with components and account roles, rules
   configured by Comptrollership. The one exception is intended by the BRD: the **editable proforma entry** of a DV
   (DIS 2.7.6) and the **ACSL correction entry** (ACSL 2.9.x), whose lines a user may change before approval; they start
   from the engine's rule output and are validated by `PostingEligibilityService`.
4. **One invoice family.** A `root_invoice_no` is carried from booking into the invoice ledger and every downstream
   document (receipt application, remittance line, DV, adjustment, ACSL correction), so that "related transactions are
   linked to a single invoice number" (DIS 3.27.2, ACSL 2.16.0) without breaking BIR invoice uniqueness (AQ29).
5. **Corrections never overwrite.** Posted journals stay immutable; corrections are linked reversing and re-posting
   entries (ACSL 2.9.1), unposted items stay editable (DIS 3.27.0 addendum).
6. **Parked means seam, not fake.** Bank channels (BOB, TPD / ACA, branch e-mail), insurer SOA formats and payroll are
   ports with manual / file adapters (section 13).

## 2. Modules

| Module | Kind | Purpose | BRD rows | Depends on | Flyway (demo) |
|---|---|---|---|---|---|
| `disbursement` | **new** | Payee master, requests (gateway / upload / encoded), DV with editable proforma, seven payment modes and instrument statuses, uploads for credited / negotiated, EOD (DCTF, checks, forms, confirmations), account funding, OR / AR and CWT tags, approval posting, cancellation and regularisation, stale / negotiated check entries, Disbursement reports. Implements `DisbursementGateway` | DIS 2.2-3.28 | opsledger (port), payables (bank accounts, cheque books, notification formatter), party, tax, accounting, journal, subledger, workflow, docgen, messaging, attachment, bulk, report, organization | V891-V893 (V999 + runner) |
| `payrequest` | **new** | Marketing refund requests (RRF) and employee cash-advance requests (RFP), ACSL / Cashiering validation tasks, Marketing and HR approvals, disbursed-check cancellation requests, CA/SA write-back, cash-advance liquidation (if confirmed, AQ18) | MKT 1.2-2.26 | opsledger (port, invoice read), crm, workflow, attachment, docgen, messaging, report | V894-V895 (runner) |
| `acsl` | **new** | Insurer SOA upload and reconciliation, GL-SL reconciliation, investigation / analysis cases, AR refund application and SL payment reversal requests, correction entries (assign / create / review / approve / post), ACSL reports | ACSL 2.2-2.16, 2.9.1 | opsledger, journal, subledger, ledger, accounting, workflow, bulk, report, messaging | V896-V897 (runner) |
| `frbs` | **new** | BDOI report pack that needs broking data (Mancom, branch production, GAP, cash flow, expense grouping), service-fee runs and monitoring | FRBS 2.10, 3.2 (part) | opsledger, booking (read), finreport, ledger, budget, report, opsledger port (payout) | V898-V899 (runner) |
| `coa`, `journal`, `period`, `closing`, `currency`, `accounting`, `report`, `receivables`, `payables`, `subledger`, `organization`, `party`, `nbadmin`, `finreport`, `tax` | platform, **changed** | FRBS, BASAU and shared changes (section 12.1) | FRBS 2.2-3.6, BASAU, DIS 2.23-2.24, 3.30.1 | as today | owners' ranges (section 4) |
| `opsledger`, `remittance`, `booking`, `crm`, `catalog` (read), `cashiering` / `commission` (planned) | broking, **changed** | Gateway contract, invoice family, CPC2 / early-incentive SI, remittance deduction, DV cancellation feedback, CA/SA (section 12.2) | DIS 3.25, 3.27.2, 3.29.x, ACSL 2.9.2, 2.16, MKT 2.25 | as today | owners' ranges |

### 2.1 Dependency graph (arrows = "depends on")

```
     payrequest     frbs          acsl                 disbursement
          \           |          /    \                /     |     \
           +----> opsledger <---+      +--> journal   /   payables  tax
                 (DisbursementGateway port,          /       (masters)
                  invoice ledger, events)  <--------+
                       |
          booking, account, catalog, crm  +  platform (accounting, subledger, workflow, bulk, docgen, messaging, report)
```

- `disbursement` implements `DisbursementGateway` (declared in `opsledger`), so it depends on `opsledger`; nothing
  depends on `disbursement`. Sources learn the outcome through `DisbursementStatusChanged` (published by
  `disbursement` using the event record declared in `opsledger`).
- `payrequest` and `frbs` send payments through the port, never through `disbursement` classes.
- `acsl` asks cashiering for application / reversal through the Operations ports `PaymentReapplier` and a new
  `PaymentReversalRequester` (declared in `opsledger`, implemented by cashiering; default adapter = ledger movement
  only, as today).
- `payrequest` asks ACSL and Cashiering for validation through `opsledger` ports `RefundValidationSource`
  (implemented by `acsl` and cashiering). This keeps the new siblings free of cycles, as in Operations.
- `tax` depends on `payables` today; `disbursement` depends on both; `tax` must not depend on `disbursement`
  (the received-certificate register lives in `tax`, and disbursement writes to it).

### 2.2 Ports added to `opsledger.service.port`

| Port | Implemented by | Called by | Purpose |
|---|---|---|---|
| `DisbursementGateway` (existing, extended) | `disbursement` (`DisbursementGatewayAdapter`, `@Primary`); queue adapter kept `@ConditionalOnMissingBean` for tests | remittance, cashiering, commission, payrequest, frbs | Send a payment request; read its status |
| `RefundValidationSource` (new) | `acsl` (premium cancelled / insurer returned), cashiering (reinstated to unapplied with new AR) | payrequest | Open a validation task for a refund of a cancelled account and report its result (MKT 1.11.0, ACSL 2.5.5) |
| `PaymentReversalRequester` (new) | cashiering | acsl | Request an SL payment reversal / re-application with approval (ACSL 2.6.0-2.6.1) |
| `InvoiceCorrectionSink` (new, implemented in opsledger itself) | opsledger | acsl | Record a CORRECTION movement on an invoice component when an ACSL correction posts |

### 2.3 Contract changes to modules already built

See section 12 for every change with files and migrations. In short: `opsledger` (Spec fields, statuses, root invoice,
ports), `remittance` (cancellation feedback, CPC2, early-incentive SI, deductions), `booking` (root invoice, SI trigger
ON_INCENTIVE), `crm` (payout accounts), `payables` (bank-account status, cheque-book edit, DCTF), `receivables` (XLSX bank
files, check-number rule), `journal` / `coa` / `period` / `closing` / `currency` / `accounting` / `report` (FRBS),
`nbadmin` (RETURNED), `party` (payee types), `subledger` (8 ageing slots), `tax` (received certificates, forms, books).

## 3. Chart of accounts: the broker chart and the demo insurer chart

**What the BRD says.** It gives no chart. FRBS sets up the chart (FRBS 2.3.0), uploads parent / child accounts from a
file (FRBS 2.3.1) and gets system-generated account numbers with roll-up (FRBS 2.3.2). BDOI's accounts are named only
through the reports of Appendices A-C (spec section 5). So **the real chart is configuration**, loaded by FRBS through the
new upload at go-live (AQ01). OQ07 stays open for the entries.

**Does a broker chart replace the demo insurer chart?** Not in the demo company, and not in code:
- the demo company `FVI` ("BrokerVerse Demo Insurance Corporation", V900) carries an **insurer** chart (UPR, claims
  reserves, reinsurance, DAC) that the underwriting, claims, reinsurance, reserves and IC demo stories need. Replacing it
  would break those demos and their tests;
- BDOI's production company gets **its own chart by upload** (FRBS 2.3.1), and the accounting rules of every broking,
  Operations and BRD-5 event are re-pointed by Comptrollership (maker-checker rules). No code refers to an account code;
- the demo broker accounts added by V988 / V990 / V994 are **placeholders** for OQ07. BRD-5 lets us name them the way
  BDOI does and fill the gaps. V999 (demo) renames and completes them as below; no code change follows because every
  module posts through events and roles.

| Demo account (added by) | Used by (events) | BDOI account named in this BRD | V999 action |
|---|---|---|---|
| 1210 + 1210.01-.06 PR by component (V988) | `BROKER_BOOKING`, `OPS_PAYMENT_APPLY`, `OPS_WRITE_OFF`, `OPS_MINIMAL_BALANCE_REVERSAL` | Premium Receivable (Peso / Dollar), aging and schedules per currency (ACSL 2.14.3) | Rename "Premium Receivable"; add 1213 PR - USD with the same components if BDOI splits by currency (AQ01); flag `revaluationRequired` on the USD accounts (FRBS 3.5.0) |
| 1211 PR - CWT 2307 (V990) | `OPS_CWT_RECLASS`, `OPS_CWT_DTIP_OFFSET` | Premiums Receivable 2307 (Appendix A IV-32) | Rename |
| 1220 Commission Receivable - Insurers (V988) | `BROKER_BOOKING`, `OPS_REMITTANCE`, `OPS_ADJ_COMMISSION`, DP commission | Commission Receivables (Peso / Dollar) | Keep; add 1221 USD (AQ01) |
| 1225 AR Insurer (V990) | `OPS_AR_INSURER_SETUP`, `OPS_AR_INSURANCE_RECEIPT` | AR Insurer's Refund | Rename; new `OPS_REMIT_DEDUCTION` credits it (ACSL 2.9.2) |
| 1230 Incentive Receivable (V990) | `OPS_INCENTIVE_ACCRUE` | (not named) | Keep |
| 1602 Creditable Withholding Tax (V900) | `OPS_REMITTANCE` (WTAX by insurer), `OPS_OR_ISSUE` | AR-BIR on commission / on incentives / on hand / tax credit (Appendix A IV-6 to 8) | Add 1610 AR-BIR on Commission, 1611 AR-BIR on Incentives, 1612 AR-BIR on Hand (certificates received), keep 1602 as tax credit; remittance and OR rules re-pointed per component (`WTAX_COMMISSION`, `WTAX_INCENTIVE`) |
| 2205 Premium Deposits / Unapplied Collections (V900) | `OPS_AR_RECEIPT`, `OPS_PAYMENT_APPLY` | AP - Others Current (Peso / Dollar): payments not yet identified or excess (Appendix A IV-18/19) | Rename "AP - Others (Unapplied Collections)" |
| 2210 Due to Insurers (DTIP) (V988) | `BROKER_BOOKING`, `OPS_REMITTANCE`, `OPS_CWT_DTIP_OFFSET`, `OPS_AR_INSURER_SETUP` | Payable to Insurance Companies (Peso / USD) | Rename; add 2212 USD (AQ01) |
| 2211 Due to Insurers - for Disbursement (V990) | `OPS_REMITTANCE`, `OPS_REMIT_INCENTIVE`, **`DISB_VOUCHER`** (type REMITTANCE) | (clearing, not named) | Keep as the remittance payable cleared by the DV |
| 2215 AP Overages (V990) | `OPS_EXCESS_TO_OVERAGES` | AP - Others (overages) | Keep |
| 2216 AP Refund Payable - Clients (V990) | `OPS_UNAPPLIED_REFUND`, **`DISB_VOUCHER`** (REFUND) | (refund payable) | Keep |
| - | **new** | A/P Refund from Insurer | Add 2217 (refund received from the insurer, payable to the client) |
| 2220 Unrealized Commission, 2221 Deferred Output VAT (V988) | `BROKER_BOOKING`, `OPS_PAYMENT_APPLY` | (not named; commission realised on collection) | Keep |
| 2230 Due to Branches - Incentive Pass-on (V990) | `OPS_INCENTIVE_PASS_ON`, **`DISB_VOUCHER`** (PASS_ON) | (not named) | Keep |
| - | **new** | Miscellaneous Liability - stale checks (DIS 3.27.1, Appendix B) | Add 2240 |
| - | **new** | Checks outstanding (clearing of issued checks until negotiated) | Add 2241 (role `@CHECK_CLEARING`) |
| - | **new** | Service fee payable (referrers' share) | Add 2250 |
| - | **new** | AP Officers and Employees; Advances to employees exist as 1604 | Add 2260; cash advances use 1604 |
| 4101 Commission Income - Brokerage (V988) | `BROKER_BOOKING`, `OPS_PAYMENT_APPLY` | Commission Income | Keep |
| 4110 Service Fee Income, 4120 Profit Share Income (V990) | `OPS_OR_ISSUE` | Service fee (income side) | Keep |
| 4130 Incentive Income (V990, under Commission Income) | `OPS_REMIT_INCENTIVE`, `OPS_INCENTIVE_ACCRUE` | Early incentive and CPC2 as **Other Income**, separate from commission (FRBS 3.1.2, DIS 3.29.2) | Re-parent 4130 under 4700 Other Income; add 4131 CPC2 Incentive Income |
| 4190 Other Income - Minimal Balance Credits (V994), 6510 Minimal Balance / Write-off (V990) | `OPS_WRITE_OFF` | (not named) | Keep |
| - | **new** | Service fee expense | Add 5614 (cost centre required) |

A **separate broker demo company** (for example `BDI` with a broker-only chart) was considered and rejected for now: it
would duplicate every demo migration of V980-V997 for a second company and give no extra coverage. The chart upload
(FRBS 2.3.1) is demonstrated with a sample file `docs/samples/coa_upload_sample.xlsx` instead.

## 4. Flyway allocation

> **Allocation decision (integration, 2026-09-25):** confirmed. BRD-5 keeps V890–V899. Collections (BRD-4) moved to
> V1000–V1009 with demo V1900–V1909. Later BRDs use V1010+ and demo V1910+ (Developer Guide range table).
>
> **Update after the BRD-3 foundation merge:** V998 is taken by the Product Maintenance demo users and V763 by
> cashiering. BRD-5 therefore uses **demo V999** only (accounts, rules, users; the storyline runs as Java demo
> runners) and **V765** for the `opsledger` extension. The references below are already updated.


Used today: V1-V27, V100-V101, V200, V300-V301, V400, V420-V421, V500, V550, V600, V650, V660, V670-V671, V700-V701,
V750-V754, V760-V762, V770-V771, V780, V790, V800-V880 (see `backend/src/main/resources/db/migration`); demo V900-V902,
V910, V920, V930, V940, V950, V955, V960-V961, V965, V970, V975, V980-V990, V992, V994. Claimed by designs not yet built:
V755, V813-V819, V821, V831, V871, V996-V997 (BRD-3); V766-V769, V772-V779, V781-V789, V991, V993, V995 (Operations
sub-ranges). Flyway runs with `out-of-order: true`.

**Proposal:**
1. **Platform changes go into the owner module's own range** (Developer Guide section 4), because they only alter
   platform tables and must run before any broking table on a fresh database.
2. **The four new modules take V890-V899**, the last free block below V900. They reference V7xx / V8xx tables only by
   plain values (ARN, invoice no., party code), like Operations.
3. **Demo takes V999** (after the Operations and BRD-3 demo) for accounts, rules and users; each new module seeds its
   demo transactions with a Java demo runner (`@Order` after `RemittanceDemoData`), as booking and remittance do.

| Version | Owner (wave) | Content |
|---|---|---|
| `V28__coa_upload_numbering_short_code.sql` | A1-GL | `coa_numbering`; unique index on (company, short_name); `coa_account.negative_balance_policy` |
| `V29__journal_assignment_reversal_links.sql` | A1-GL | `jnl_batch.assigned_to`, `reverse_on`, `corrects_batch_id`, `related_invoice_no`, `root_invoice_no`; job row `JOURNAL_AUTO_REVERSAL` |
| `V30__period_module_lock_close_schedule.sql` | A1-GL | `acc_period_module_lock`; `acc_period_close_schedule` |
| `V31__cost_center_rules_and_employees.sql` | A1-GL | `acc_cost_center_rule`; `org_employee`; alert `COST_CENTER_MISSING` |
| `V32__report_batch_print_options.sql` | A1-GL | `report_batch`, `report_batch_item`; print-option columns on `report_run` |
| `V33__party_payee_types.sql` | A1-GL | Party types EMPLOYEE, GOVERNMENT, OTHER_PAYEE (check constraint) |
| `V402__closing_year_end_verification.sql` | A1-GL | `yec_year_end_close` verification columns; parameters `CLOSE_ONLY_PREVIOUS_MONTH`, `YEAR_END_CLOSE_DEADLINE` |
| `V502__payables_bank_status_dctf.sql` | A1-DSB | `pay_bank_account.status` active / inactive; `pay_cheque_book` edit audit; notification format DCTF |
| `V551__bank_statement_layouts_rules.sql` | A1-GL | `brs_statement_layout`; `brs_match_rule` per bank account |
| `V652__account_schedule_engine.sql` | A1-FRBS | `fin_schedule_def`, `fin_schedule_column`, `fin_statement_comment` |
| `V702__tax_received_certificates.sql` | A1-FRBS | `tax_certificate_received` (+ lines), SAWT source |
| `V703__tax_books_and_forms.sql` | A1-FRBS | Forms 0619-F, 1603, 1702-Q, 1702, 1604-E, MAP, SAWT, IC broker statement; books-of-accounts definitions |
| `V765__ops_gateway_v2_root_invoice.sql` | A0 | `ops_invoice.root_invoice_no`; `ops_disbursement_request` new columns (rfp_no, payee_class, disbursement_type, dv_status); movement type CORRECTION |
| `V772__remittance_cpc2_deduction_si.sql` | A1-OPSX | `rem_batch_line.cpc2_*`; `rem_deduction`; events `OPS_REMIT_CPC2`, `OPS_REMIT_DEDUCTION`; SI link on batch |
| `V791__access_request_returned.sql` | A1-GL | `RETURNED` status on access requests |
| `V802__crm_client_payout_account.sql` | A1-PRQ | `crm_client_payout_account` |
| `V872__booking_root_invoice_incentive_si.sql` | A1-OPSX | `bkg_invoice.root_invoice_no` (backfill from `parent_invoice_no` chains); SI type EARLY_INCENTIVE, trigger ON_INCENTIVE |
| `V890__acct_foundation.sql` | A0 | All new permissions and role grants, new roles, LOV types, workflow definitions (`wf_stage`, `wf_transition`) of the four modules, parameters, alert codes, report categories |
| `V891__disbursement_payees_requests.sql` | A1-DSB | `dsb_payee`, `dsb_payee_account`, `dsb_payee_request`, `dsb_request` |
| `V892__disbursement_vouchers_instruments.sql` | A1-DSB | `dsb_voucher`, `dsb_voucher_line`, `dsb_instrument`, `dsb_instrument_event`, `dsb_eod_run`, `dsb_funding_request`, `dsb_voucher_tag` |
| `V893__disbursement_events_templates.sql` | A1-DSB | Event types `DISB_*`, document templates (DV, ATD, MC / DD, CT, TT, check), bulk handler codes |
| `V894__payrequest.sql` | A1-PRQ | `prq_request`, `prq_request_line`, `prq_validation`, `prq_liquidation` |
| `V895__payrequest_templates.sql` | A1-PRQ | RRF / RFP / liquidation templates |
| `V896__acsl.sql` | A1-PRQ | `acsl_case`, `acsl_soa_upload`, `acsl_soa_line`, `acsl_recon_run`, `acsl_recon_result`, `acsl_correction`, `acsl_correction_line`, `acsl_glsl_recon` |
| `V897__acsl_events_reports.sql` | A1-PRQ | Event `ACSL_CORRECTION`; report definitions metadata |
| `V898__frbs_service_fee.sql` | A1-FRBS | `frbs_service_fee_rule`, `frbs_service_fee_run`, `frbs_service_fee_line` |
| `V899__frbs_events.sql` | A1-FRBS | Events `FRBS_SERVICE_FEE_ACCRUE`; report metadata |
| `V999__demo_acct_chart_rules_users.sql` | A0 | Demo accounts and renames of section 3, demo rules of every new event, demo users of section 6 |
| `V999__demo_acct_masters.sql` | A1-DSB | Demo payees, bank-account statuses, cheque books, employees, cost-centre rules, statement layouts |

Rules:
- No foreign keys from V28-V33, V402, V502, V551, V652, V702-V703 to broking tables.
- V890 references only V1-V754 tables (roles, permissions, LOV, workflow), so it is safe before V800 on a fresh database.
- The Developer Guide range table gets: "V890-V899 Accounting, Disbursement and ACSL (BRD-5); demo V999".
- After V999 the demo range is full. The next BRD should open **V1000-V1099 (schema) and V1900-V1999 (demo, `db/demo`)**
  and write that convention into the Developer Guide; this BRD does not need it.
- Alternative rejected: V1000+ now. It would work (`db/demo` is a separate location and ordering is by version), but it
  would split BRD-5 away from the documented "demo = V900-V999" rule when free versions still exist.

## 5. Entities (key fields)

Money `numeric(19,2)`, rates `numeric(19,8)`. All tables have the base audit columns; masters extend
`AuthorizableEntity` (maker-checker).

### 5.1 `disbursement`

- `dsb_payee`: party_code (FK by value to `party`), payee_class (LOV `PAYEE_CLASS`: SUPPLIER / INSURER / EMPLOYEE /
  CLIENT / GOVERNMENT / OTHER), name, address, default mode (`DisbursementMode`), allowed modes, disbursement types,
  currency, default cost centre, source (MANUAL / UPLOAD / MIGRATION / REQUEST), record status (DRAFT / ACTIVE /
  INACTIVE). Taxes come from `tax_party_profile` (TIN, ATC, VAT).
- `dsb_payee_account`: payee, bank, branch, account no. (masked in lists), account name, currency, mode (CTA / TT /
  ONLINE), primary flag, active.
- `dsb_payee_request`: request for a new / changed payee (source RRF, disbursement, no-match), payload, status.
- `dsb_request`: request_no, source (GATEWAY / UPLOAD / ENCODED / PAYREQUEST), source module / ref, RFP no.,
  disbursement type (REMITTANCE / REFUND / SUPPLIER / GOVERNMENT / OTHER_BANK_UNIT / EMPLOYEE / CASH_ADVANCE /
  SERVICE_FEE / PASS_ON / CWT2307 / OTHER), payee code, amount, currency, received_at, attachments, status (RECEIVED /
  REJECTED_NO_PAYEE / IN_VOUCHER / CANCELLED), linked `ops_disbursement_request` id (gateway requests).
- `dsb_voucher` (DV): dv_no `DV-<yyyy>-nnnnnn`, request(s), payee, mode, bank account (paying), currency, amount, rate
  (BOOK for USD), gross / EWT / net, purpose, root_invoice_no(s), stage (IN_PROCESS / FOR_REVIEW / FOR_APPROVAL / APPROVED
  / CANCELLED / REJECTED), proforma_edited flag, journal batch no., approved_by / at, cancellation reason, cancel journal.
- `dsb_voucher_line`: proforma lines (side, account, party, cost centre, business line, amount, component, from_rule /
  edited).
- `dsb_instrument`: DV, mode, instrument no. (check no., ATD no., MC / DD no., CT / TT ref, CTA extraction ref, BOB ref),
  status (per section 7.2), printed_at, released_at / to, credited_at, debited_at, negotiated_at, stale_at, eod_run.
- `dsb_instrument_event`: status history with source (USER / SYSTEM / UPLOAD / JOB) and file id.
- `dsb_voucher_tag`: kind (OR_AR / CWT), OR / AR no., date, date received; CWT period covered, date received /
  released, amount, certificate id (`tax_certificate_received` or `tax_2307_certificate`).
- `dsb_status_edit`: requested status change, reason, approval.
- `dsb_eod_run`: business date, outputs (DCTF file, check batch, forms, reports), counts, e-mails sent.
- `dsb_funding_request`: `FND-<yyyy>`, source account, target account (main BDOIR account), amount, purpose, maker,
  verifier, approver 1, approver 2, BOB reference, status, journal.

### 5.2 `payrequest`

- `prq_request`: request_no `RRF-<yyyy>` / `RFP-<yyyy>`, kind (REFUND / CASH_ADVANCE / CHECK_CANCELLATION), segment,
  requester (AO / employee), unit, payee (client / employee), mode (CTA / CHECK / ...), CA/SA fields (payee name, BDO
  account no.), amount, currency, purpose, stage, assigned_to, disbursement request no., DV no., disbursed_at.
- `prq_request_line` (RRF): AR no. (unique among live refunds, MKT 2.23.0), client no., assured, invoice / root invoice,
  amount, reason (LOV `REFUND_REASON`), branch / unit, category A, category B, account / check name.
- `prq_validation`: request, validator (ACSL / CASHIERING), status (OPEN / CONFIRMED / REJECTED), result, new AR no.
- `prq_liquidation` (if confirmed, AQ18): cash advance, lines (date, particulars, per diem, representation, transport,
  lodging, others), over / short, status.

### 5.3 `acsl`

- `acsl_case`: case_no `ACS-<yyyy>`, type (INVESTIGATION / ANALYSIS_REQUEST / CORRECTION / REFUND_APPLICATION /
  PAYMENT_REVERSAL), invoice / root invoice, insurer, requester (module / user), findings, result, stage, assigned_to.
- `acsl_soa_upload`: insurer, period, file, sha256, rows read / loaded / failed (upload log, ACSL 2.4.0), bulk job.
- `acsl_soa_line`: insurer fields (invoice, policy, assured, dates, gross, balance, payments), matched invoice.
- `acsl_recon_run` / `acsl_recon_result`: per invoice: outstanding, for remittance, remitted (batch, date), 2307 amount /
  batch / date, cancelled (+ cancellation reference), direct billed, SOA balance, not found, premium variance, outstanding
  variance.
- `acsl_correction` / `acsl_correction_line`: case, kind (WRONG_ACCOUNT / AMOUNT / RECLASS / OTHER), original journal
  batch and line, lines (account, party, invoice, component, cost centre, amount), stage, journal batch no.
- `acsl_glsl_recon`: run date, control account, GL balance, SL balance, difference.

### 5.4 `frbs`

- `frbs_service_fee_rule`: segment, rate (2.5% / 1%), base (COMMISSION_FULLY_PAID), tax treatment, effective dates.
- `frbs_service_fee_run`: period, status (COMPUTED / APPROVED / RELEASED / LIQUIDATED), totals.
- `frbs_service_fee_line`: unit / referrer (party), base commission, rate, gross, taxes, net, disbursement request no.,
  released date, liquidation date, liquidation report (attachment).

### 5.5 Platform additions (section 12.1)

`coa_numbering`, `acc_period_module_lock`, `acc_period_close_schedule`, `acc_cost_center_rule`, `org_employee`,
`report_batch`, `brs_statement_layout`, `brs_match_rule`, `fin_schedule_def` / `fin_schedule_column` /
`fin_statement_comment`, `tax_certificate_received`, `crm_client_payout_account`, `rem_deduction`.

## 6. Accounting events and default GL entries

Demo rules only (V999); the accounts are the demo chart of section 3. The real rules come from Comptrollership (AQ02).
USD documents are priced at the BOOK rate (Operations convention). Source references are idempotent keys.

| # | Transaction | Event (source ref) | Default entry (demo) |
|---|---|---|---|
| 1 | DV approved - remittance to insurer | `DISB_VOUCHER` type REMITTANCE (`DV:<no>`) | Dr 2211 Due to insurer for disbursement (insurer) / Cr `@PAY_ACCOUNT` (bank, or 2241 for checks) |
| 2 | DV approved - refund to client | `DISB_VOUCHER` REFUND | Dr 2216 AP refund payable (client) / Cr `@PAY_ACCOUNT` |
| 3 | DV approved - refund received from insurer | `DISB_VOUCHER` REFUND_FROM_INSURER | Dr 2217 AP refund from insurer (client) / Cr `@PAY_ACCOUNT` |
| 4 | DV approved - supplier / government / other bank unit | `DISB_VOUCHER` SUPPLIER / GOVERNMENT / OTHER_BANK_UNIT | Dr 2501 AP suppliers (payee; matches supplier-invoice open items) or the payable of the request / Cr `@PAY_ACCOUNT`; Cr 2508 EWT when withheld at payment (AQ13) |
| 5 | DV approved - employee / cash advance | `DISB_VOUCHER` EMPLOYEE / CASH_ADVANCE | Dr 1604 Advances to employees (employee) or the expense (`@EXPENSE`, cost centre) / Cr `@PAY_ACCOUNT` |
| 6 | DV approved - service fee / pass-on | `DISB_VOUCHER` SERVICE_FEE / PASS_ON | Dr 2250 / 2230 / Cr `@PAY_ACCOUNT` |
| 7 | Proforma edited | same event | The edited lines are posted as given (validated, balanced); the DV shows "edited" |
| 8 | Check negotiated (file) | `DISB_CHECK_NEGOTIATED` (`CHK:<id>:NEG`) | Dr 2241 Checks outstanding / Cr bank (when the check model is on, parameter `DISB_CHECK_CLEARING=ON`) |
| 9 | Check staled (180 days) | `DISB_CHECK_STALE` (`CHK:<id>:STALE`) | Dr 2241 / Cr 2240 Miscellaneous Liability - stale checks (payee) |
| 10 | Re-issue of a stale check | `DISB_VOUCHER` type STALE_REISSUE | Dr 2240 / Cr `@PAY_ACCOUNT` |
| 11 | Approved DV cancelled | the DV event with negative amounts (`DV:<no>:CANCEL`) | Reverses 1-6; open items unmatched; sources restored (section 12.2) |
| 12 | Funding of the main account (BOB) | `DISB_FUND_TRANSFER` (`FND:<no>`) | Dr target bank / Cr source bank (inter-bank) |
| 13 | CWT 2307 batch released to insurer with the remittance | cashiering `OPS_CWT_DTIP_OFFSET` triggered by the DV status | Dr 2210 / Cr 1211 (Operations row 4) |
| 14 | Insurer certificate on commission received (tag) | `TAX_CWT_CERT_RECEIVED` (`CRT:<id>`) | Dr 1612 AR-BIR on hand / Cr 1610 AR-BIR on commission (or 1611 on incentives) |
| 15 | CPC2 incentive per remittance batch | `OPS_REMIT_CPC2` (`RMB:<batch>:CPC2`) | Dr 2211 / Cr 4131 CPC2 incentive income (+ Cr 2504 output VAT) |
| 16 | Early incentive with automatic SI | `OPS_REMIT_INCENTIVE` (existing) + SI | Dr 2211 / Cr 4130 (Other Income) + output VAT; the insurer's 2% WTAX: Dr 1611 / Cr 2211 at collection (AQ25) |
| 17 | Remittance deduction on insurer confirmation | `OPS_REMIT_DEDUCTION` (`DED:<id>`) | Dr 2211 (or 2210) / Cr 1225 AR insurer's refund (insurer) |
| 18 | ACSL correction (wrong account, amount, reclass) | `ACSL_CORRECTION` posted as system journal ADJUSTMENT (`ACS:<case>`), lines from the correction | Reversal of the original line(s) + re-post to the right account; open items recorded / matched for party lines; ops movement CORRECTION |
| 19 | SL payment reversal (ACSL 2.6.1) | cashiering reversal events (Operations rows 2 / 9) | Dr 1210.x / Cr 2205 (payment back to unapplied) |
| 20 | Service fee accrual | `FRBS_SERVICE_FEE_ACCRUE` (`SFR:<run>`) | Dr 5614 service fee expense (cost centre) / Cr 2250 service fee payable (unit / referrer) |
| 21 | Cash-advance liquidation (if in scope) | `PRQ_CA_LIQUIDATION` (`LIQ:<no>`) | Dr expenses by category (`@PER_DIEM`, `@REPRESENTATION`, `@TRANSPORT`, `@LODGING`, `@OTHER`, cost centre) / Cr 1604; over: Dr cash / Cr 1604; short: Dr 1604 / Cr 2260 AP employees |
| 22 | Accrual auto-reversal (FRBS 2.8.1) | system journal REVERSAL (`JV:<no>:AUTOREV`) | Mirror of the accrual on `reverse_on` |

Sub-ledger: DV approval records a DEBIT `PAYMENT` open item of the payee and matches the paid CREDIT items (refund
payable, AP, DTIP for disbursement), as payables does; cancellation unmatches (payables pattern). Stale checks open a
CREDIT item of the payee in 2240.

## 7. Workflows (seeded in V890)

`(perm)` is the permission of a transition.

### 7.1 `DISB_VOUCHER` (DIS 2.7-2.21)

```
IN_PROCESS(DISB_PROCESS) --submit--> FOR_REVIEW(DISB_REVIEW) --submit_for_approval--> FOR_APPROVAL(DISB_APPROVE) --approve--> APPROVED [posted]
FOR_REVIEW/FOR_APPROVAL --return(reason)--> IN_PROCESS
FOR_APPROVAL --reject(reason)--> REJECTED [terminal]
IN_PROCESS --cancel(reason, DISB_PROCESS)--> CANCELLED ; FOR_REVIEW --cancel(reason, DISB_REVIEW)--> CANCELLED
APPROVED --cancel(reason, DISB_APPROVE)--> CANCELLED [reversal + regularisation]
Gateway REFUND / REMITTANCE with a maintained payee: created directly in FOR_APPROVAL (DIS 3.25.0)
```

### 7.2 Instrument statuses (DIS 2.8, 3.26)

```
CHECK:         PRINTED --release--> RELEASED --negotiated(file)--> NEGOTIATED ; PRINTED/RELEASED --180 days--> STALE ; any --cancel--> CANCELLED
ATD:           PRINTED(pdf) --email--> EMAILED --branch_confirmed--> DEBITED
CTA:           EXTRACTED(DCTF) --credited(txt)--> CREDITED
CREDIT_TICKET/TT: PRINTED --branch_confirmed--> DEBITED
MC_DD:         PRINTED --received_from_branch--> RECEIVED --release--> RELEASED
ONLINE_BANKING: APPROVED --bob_approved(voucher ref)--> DEBITED
Manual edits of a status: DISB_STATUS_EDIT  REQUESTED(DISB_PROCESS) --approve(DISB_STATUS_APPROVE)--> APPLIED ; --reject--> REJECTED
```

Mapping to the Operations gateway statuses: IN_PROCESS -> ACKNOWLEDGED; APPROVED -> DV_ASSIGNED (DV no. known);
RELEASED / CREDITED / DEBITED / NEGOTIATED -> PAID; REJECTED -> RETURNED; CANCELLED -> **CANCELLED** (new).

### 7.3 Others

- `DISB_FUNDING` (DIS 2.17): `CREATED(DISB_FUNDING_REQUEST) --submit--> FOR_VERIFICATION(DISB_FUNDING_VERIFY, not the
  maker) --verify--> FOR_APPROVAL_1(DISB_FUNDING_APPROVE) --approve--> FOR_APPROVAL_2(DISB_FUNDING_APPROVE, other user)
  --approve--> APPROVED`; `decline` from either approval -> DECLINED; `return(reason)` -> CREATED.
- `DISB_PAYEE` (DIS 2.2): `DRAFT -> FOR_AUTHORIZATION(DISB_PAYEE_AUTHORIZE) -> ACTIVE`; deactivate / reactivate the same way.
- `PRQ_REFUND` (MKT): `DRAFT(PRQ_CREATE) --assign(PRQ_ASSIGN)--> PREPARING --submit--> [VALIDATION: ACSL + CASHIERING tasks,
  cancelled policies only] --> FOR_REVIEW(PRQ_REVIEW) --> FOR_APPROVAL(PRQ_APPROVE) --approve--> SENT_TO_DISBURSEMENT
  --disbursed(system)--> DISBURSED`; `return` to the previous handler; `cancel` before approval.
- `PRQ_CASH_ADVANCE`: as refund without validation, plus `HR_APPROVAL(PRQ_HR_APPROVE)` after Marketing approval.
- `PRQ_CHECK_CANCEL` (MKT 1.19.0 / 1.16.3): `REQUESTED -> FOR_REVIEW -> FOR_APPROVAL -> SENT` (Disbursement cancels the
  approved DV, DIS 2.20.0).
- `ACSL_CASE`: `RECEIVED -> ASSIGNED(ACSL_ASSIGN) -> INVESTIGATING(ACSL_PROCESS) -> RESULT_PROVIDED [terminal]` or
  `-> CORRECTION` (spawns a correction).
- `ACSL_CORRECTION` (ACSL 2.7-2.15, 2.9.1): `ASSIGNED(ACSL_ASSIGN) -> DRAFT(ACSL_PROCESS) --submit--> FOR_REVIEW(ACSL_REVIEW)
  --endorse--> FOR_APPROVAL(ACSL_APPROVE) --approve--> POSTED`; `return(comment)` from review / approval to DRAFT.
- `REM_DEDUCTION` (ACSL 2.9.2, in remittance): `DRAFT(ACSL_PROCESS) --upload insurer confirmation--> FOR_CONFIRMATION
  (REMIT_DEDUCTION_CONFIRM) --confirm--> CONFIRMED --consumed by batch--> APPLIED`.
- `FRBS_SERVICE_FEE`: `COMPUTED(SERVICE_FEE_MANAGE) -> APPROVED(SERVICE_FEE_APPROVE) -> RELEASED(system / tag) -> LIQUIDATED(tag)`.

## 8. Security

### 8.1 Permissions (added to `security.domain.Permission` in A0; granted in V890)

| Permission | Used for |
|---|---|
| `JOURNAL_ASSIGN` | Assign / re-assign manual entries for posting (FRBS 2.5.1) |
| `REVALUATION_RATE_MAINTAIN` | Monthly revaluation rate (FRBS 2.2.0; Section Head per matrix) |
| `COA_UPLOAD` | Chart upload (FRBS 2.3.1) |
| `GL_CLOSE_SCHEDULE` | Schedule / run the month-end and year-end close (TL per matrix; with `PERIOD_MANAGE`) |
| `FRBS_REPORT_VIEW`, `FRBS_REPORT_EXPORT` | BDOI report pack |
| `SERVICE_FEE_MANAGE`, `SERVICE_FEE_APPROVE`, `SERVICE_FEE_TAG` | Service-fee runs and tagging |
| `DISB_VIEW` | Disbursement read |
| `DISB_PAYEE_MAINTAIN`, `DISB_PAYEE_AUTHORIZE`, `DISB_PAYEE_VIEW_FULL` | Payee master (full account numbers) |
| `DISB_UPLOAD` | Request and status uploads |
| `DISB_PROCESS` (exists) | Encode, process, tag, cancel In Process |
| `DISB_REVIEW` | Review / check, submit for approval, cancel For review (TL) |
| `DISB_APPROVE` | Approve / decline / reject, cancel Approved |
| `DISB_STATUS_APPROVE` | Approve status edits |
| `DISB_EOD` | End-of-day processing |
| `DISB_FUNDING_REQUEST`, `DISB_FUNDING_VERIFY`, `DISB_FUNDING_APPROVE` | Account funding |
| `DISB_TAG` | OR / AR and CWT tagging |
| `DISB_REPORT_VIEW`, `DISB_REPORT_EXPORT` | Disbursement reports |
| `PRQ_CREATE`, `PRQ_ASSIGN`, `PRQ_REVIEW`, `PRQ_APPROVE`, `PRQ_HR_APPROVE`, `PRQ_VIEW` | Marketing refund / cash-advance requests |
| `ACSL_VIEW`, `ACSL_UPLOAD`, `ACSL_PROCESS`, `ACSL_ASSIGN`, `ACSL_REVIEW`, `ACSL_APPROVE`, `ACSL_APPLY` | ACSL |
| `ACSL_REPORT_VIEW`, `ACSL_REPORT_EXPORT` | ACSL reports |
| `REMIT_DEDUCTION_CONFIRM` | Confirm a remittance deduction (ACSL) |
| `EMPLOYEE_MAINTAIN` | Employee / cost-centre master (DIS 3.30.1) |

### 8.2 Roles (V890) and demo users (V999, password `Brokerverse@2026`)

Grants follow the role matrices read from the scanned pages (pp.181, 216-217, 227, 240); final matrix AQ28.

| Role | Persona | Key permissions | Demo user |
|---|---|---|---|
| `FRBS_PROCESSOR` | GL Officer (Processor) | JOURNAL_CREATE, JOURNAL_VIEW, SERVICE_FEE_MANAGE, SERVICE_FEE_TAG, FRBS_REPORT_VIEW / EXPORT | `glofficer` |
| `FRBS_TL` | GL Team Lead | + JOURNAL_AUTHORIZE, JOURNAL_ASSIGN, PERIOD_MANAGE, PERIOD_END_RUN, GL_CLOSE_SCHEDULE, MASTER_MAINTAIN (chart), COA_UPLOAD, SERVICE_FEE_APPROVE | `gltl` |
| `FRBS_HEAD` | GL Team Head / Section Head | + REVALUATION_RATE_MAINTAIN, MASTER_AUTHORIZE, JOURNAL_REVERSE | `glhead` |
| `DISBURSEMENT` (existing role, kept as the processor role) | Disbursement Processor | DISB_VIEW, DISB_PROCESS, DISB_UPLOAD, DISB_TAG, DISB_REPORT_VIEW / EXPORT, TAX_VIEW (2307) | `disb` (exists) |
| `DISB_TL` | Disbursement Team Leader | + DISB_REVIEW, DISB_PAYEE_MAINTAIN, DISB_FUNDING_REQUEST, DISB_FUNDING_VERIFY, DISB_EOD, DISB_STATUS_APPROVE | `disbtl`, `disbtl2` (verifier) |
| `DISB_APPROVER` | Disbursement Approver | DISB_APPROVE, DISB_PAYEE_AUTHORIZE, DISB_FUNDING_APPROVE, DISB_EOD, MASTER_MAINTAIN (bank accounts, check series) | `disbappr`, `disbappr2` |
| `PRQ_PROCESSOR` | Marketing AO / processor | PRQ_CREATE, PRQ_VIEW, OPS_REPORT_VIEW (unapplied payments) | `mktao` |
| `PRQ_REVIEWER` | Marketing reviewer | + PRQ_ASSIGN, PRQ_REVIEW | `mktrev` |
| `PRQ_APPROVER` | Marketing approver | PRQ_APPROVE, PRQ_VIEW | `mktappr` |
| `HR_APPROVER` | Human Resources | PRQ_HR_APPROVE, PRQ_VIEW | `hrappr` |
| `ACSL_PROCESSOR` | ACSL Processor | ACSL_VIEW, ACSL_UPLOAD, ACSL_PROCESS, ACSL_APPLY, ACSL_REPORT_VIEW / EXPORT | `acsl` |
| `ACSL_TL` | ACSL Team Leader | + ACSL_ASSIGN, ACSL_REVIEW, REMIT_DEDUCTION_CONFIRM | `acsltl` |
| `ACSL_HEAD` | ACSL Team Head (Approver) | ACSL_APPROVE, ACSL_VIEW, ACSL_REPORT_VIEW | `acslhead` |
| `BUSINESS_ADMIN`, `SYSTEM_ADMIN`, `SYSADMIN_APPROVER` | BASAU | Existing LOV / access-request permissions | existing admin users |

Segregation rules enforced in services: the funding verifier differs from the maker, the two approvers differ from each
other and from the maker; a DV approver is not its processor; an ACSL approver is not the correction's maker.

## 9. Jobs, alerts, parameters, LOVs, numbers, templates, bulk handlers

| ManagedJob | Module | Default cron (UTC) | BRD |
|---|---|---|---|
| `JOURNAL_AUTO_REVERSAL` | journal | daily 16:05 (00:05 PHT) | FRBS 2.8.1 |
| `GL_PERIOD_CLOSE` | closing | per `acc_period_close_schedule` (checked every 15 min) | FRBS 2.6.0 |
| `BROKING_BOOKS_CLOSE` | closing | month end at `BROKING_CLOSE_TIME` (default 23:00 PHT) | FRBS 3.4.1 |
| `BOOK_RATE_FROM_CLOSING` | currency | 1st of month 00:30 PHT | OQ08 proposal |
| `DISB_CHECK_STALE` | disbursement | daily 00:20 PHT | DIS 3.26.2 |
| `DISB_EOD_CONFIRMATION` | disbursement | after each EOD run | DIS 2.7.12 |
| `DISB_EOD_REPORTS` | disbursement | after each EOD run | DIS 3.28.0 |
| `ACSL_GL_SL_RECON` | acsl | daily 20:00 PHT and at period end | ACSL 2.13.2 |

Alerts: `COST_CENTER_MISSING`, `YEAR_END_CLOSE_DUE`, `GL_CLOSE_FAILED`, `DISB_PAYEE_NO_MATCH`, `DISB_UNREGULARIZED`,
`DISB_CHECK_STALE`, `ACSL_GLSL_DIFFERENCE`, `CHECK_SERIES_LOW`.

Parameters: `CLOSE_ONLY_PREVIOUS_MONTH` (true), `YEAR_END_CLOSE_DEADLINE` (04-15), `BROKING_CLOSE_TIME` (23:00),
`OPS_BOOK_RATE_SOURCE` (CLOSING_PREV_MONTH), `DISB_STALE_DAYS` (180), `DISB_CHECK_CLEARING` (ON),
`DISB_AUTO_APPROVER_ROUTING` (REFUND, REMITTANCE), `EARLY_INCENTIVE_WTAX_RATE` (2), `AGEING_BUCKETS` (BDOI:
30,90,180,365,730).

LOV types (V890): `PAYEE_CLASS`, `DISBURSEMENT_TYPE`, `DISB_CANCEL_REASON`, `DISB_RETURN_REASON`, `BRANCH_EMAIL`,
`REFUND_REASON`, `RRF_CATEGORY_A`, `RRF_CATEGORY_B`, `ACSL_CASE_TYPE`, `ACSL_CORRECTION_KIND`, `SERVICE_FEE_SEGMENT`.

Numbers: `DV-<yyyy>`, `DSR-<yyyy>` (requests), `FND-<yyyy>`, `RRF-<yyyy>`, `RFP-<yyyy>`, `ACS-<yyyy>`, `SFR-<yyyy>`.

Document templates (docgen): `DSB_VOUCHER`, `DSB_ATD`, `DSB_MC_DD`, `DSB_CREDIT_TICKET`, `DSB_TT`, `DSB_CHECK_<bank>`,
`DSB_PAYMENT_ADVICE`, `PRQ_RRF`, `PRQ_RFP`, `PRQ_LIQUIDATION`, `SI_EARLY_INCENTIVE`.

Bulk handlers: `COA_ACCOUNTS` (coa, COA_UPLOAD), `DISB_REQUESTS`, `DISB_CHECKS_NEGOTIATED`, `DISB_CTA_CREDITED`,
`DISB_BOB_APPROVED`, `DISB_PAYEE_MIGRATION`, `DSB_EXPENSE_ALLOCATION` (disbursement), `ACSL_INSURER_SOA` (acsl),
`BANK_STATEMENT_XLSX` (receivables).

## 10. Reports

| Module | Codes |
|---|---|
| disbursement (Appendix B) | `DSB-MASTERLIST`, `DSB-UNRELEASED-CHECKS`, `DSB-CWT-COMMISSION`, `DSB-ATD`, `DSB-ML-STALE`, `DSB-CASH-FLOW`, `DSB-PAYEE`, `DSB-PAYEE-NOMATCH`, `DSB-UPLOAD-FALLOUT`, `DSB-EOD-REMIT`, `DSB-EOD-REFUND`, `DSB-EOD-SUMMARY`, `DSB-EOD-SUPPLIER`, `DSB-EOD-EMPLOYEE`, `DSB-EOD-OTHER`, `DSB-UNREGULARIZED`; DCTF file |
| remittance | `DSB-CPC2-INCENTIVE` (owned by remittance; category Disbursement) |
| acsl (Appendix C) | `ACSL-SOA-RECON`, `ACSL-SOA-UPLOAD-LOG`, `ACSL-BOOKED-FIN-DETAILS`, `ACSL-GL-SL-RECON`, `ACSL-AGING-{PAY-INS-PHP, PAY-INS-USD, PR-PHP, PR-USD, AP-REFUND-INS, AR-INS-REFUND, COMM-PHP, COMM-USD}`, `ACSL-SCHED-{PAY-INS-PHP, PAY-INS-USD, AP-REFUND-INS, AR-INS-REFUND, COMM-PHP, COMM-USD}` |
| payrequest | `PRQ-STATUS`, `PRQ-REGISTER` |
| frbs (Appendix A V, VI and broker parts of IV) | `FRBS-MANCOM-MARKET`, `FRBS-BRANCH-PRODUCTION` (detail / summary), `FRBS-SERVICE-FEE` (summary / detail), `FRBS-EXPENSE-GROUPING`, `FRBS-GAP`, `FRBS-CASH-FLOW` |
| finreport (schedule engine) | `GL-SCHEDULE` driven by `fin_schedule_def`; Appendix A II-IV schedules seeded as definitions `GARD-*`, `SUBS-*`, `SCH-*` (V652 seeds the list with "layout to confirm", AQ05) |
| tax | `TAX-0619F`, `TAX-1603`, `TAX-1702Q`, `TAX-1702`, `TAX-1604E`, `TAX-MAP`, `TAX-SAWT`, `TAX-BOOK-{GJ, PJ, SJ, CRB, CDB, SL}`, `IC-BROKER-ASBO` |
| organization | `ORG-HEADCOUNT-CC` |

Report platform changes: report batch (select several, shared parameters, async, ZIP / merged PDF), column filters in the
viewer, PDF print options (paper, orientation, fit to width).

## 11. Screens (BDOI navigation groups)

Record pages follow the broking pattern (header with reference chip and status, `WorkflowPanel`, tabs, permitted actions);
lists use `DataTable` with saved filters.

- **Finance** group (existing), after Remittance / Commission:
  - **Disbursement** (`features/disbursement`): Workbench (tabs System requests / In process / For review / For approval
    / Approved / Cancelled), **DV page** (Details, Entry (proforma editor with "from rule / edited" markers), Instrument
    (status timeline), Tags (OR / AR, CWT), Documents, History), Encode request, Uploads (requests, deposited checks,
    credited accounts, BOB approvals), **End of day** (run for a date; outputs with download, check print batch, e-mail
    log), Payees (list with active / inactive, payee page, requests), Account funding, Check series and bank accounts
    (links to the payables masters), Reports.
  - **ACSL** (`features/acsl`): Cases board (analysis requests, investigations, corrections by stage), Correction editor
    (original lines of the invoice's journals on the left, correction on the right, balance check), SOA uploads with the
    upload log and reconciliation result tabs (buckets as tabs, variance highlight), GL-SL reconciliation, Reports.
  - **General Ledger** (existing `features/gl`): chart upload wizard, short-code entry on journal lines, "reverse on"
    date, assigned-to-me filter, bulk approve, confirmation dialogs; **Planning & Closing** (`features/closing`): close
    schedule, broking-books cut-off status, year-end verification panel; revaluation rate entry on Currency Rates.
  - **Accounting reports** (`features/frbs`): BDOI report pack grouped as Appendix A (EOD, GARD, Subsidiaries, Schedules,
    Mancom, Service fee, Government), report batch; **Service fee** runs and tagging.
- **Operations** group: **Refund & Cash Advance Requests** (`features/payrequest`) for Marketing AO / reviewer / approver
  and HR (request list, RRF / RFP forms, validation tasks, status tracker); the Operations Disbursement queue screen is
  removed when `disbursement` is active.
- **Setup & Administration**: Employees / cost centres (DIS 3.30.1), cost-centre rules (FRBS 3.1.1), bank statement
  layouts, schedule definitions.

Help sections are pre-registered by A0 in `help/helpContent.ts`; each feature owns its `help.ts`.

## 12. Impact on modules already built or being built

### 12.1 Platform (finance) modules

| Module | Change | BRD | Files |
|---|---|---|---|
| `coa` | Upload handler; numbering scheme; unique short code and search; `negative_balance_policy` | FRBS 2.3.1-2.3.3, 2.5.4 | `coa/domain/GlAccount.java`, `coa/service/ChartOfAccountsService.java`, new `coa/service/CoaUploadHandler.java`, V28 |
| `journal` | `assigned_to`, `reverse_on` + job, `corrects_batch_id` / `related_invoice_no` / `root_invoice_no` on `SystemJournalRequest` and `JournalBatch`; negative-balance check in `JournalValidator`; bulk approve | FRBS 2.5.x, 2.8.x, ACSL 2.9.1 | `journal/domain/JournalBatch.java`, `journal/service/JournalValidator.java`, `JournalAuthorizationService.java`, `SystemJournalRequest.java`, new `JournalAutoReversalJob.java`, V29 |
| `period`, `closing` | Module cut-off (`requirePostingPeriod(date, module)` overload; existing callers unchanged); close schedule job; previous-month guard; year-end verification; deadline alert | FRBS 2.6.x, 2.7.x, 3.4.x | `period/service/PeriodService.java`, `closing/service/ClosingChecklistService.java`, `YearEndService.java`, V30, V402 |
| `currency` | BOOK-from-CLOSING job (parameterised) | FRBS 2.2.0, OQ08 | `currency/service/CurrencyService.java`, V30 (job row) |
| `accounting` | Cost-centre derivation rules applied in `AccountingEngine` before rule resolution; `COST_CENTER_MISSING` | FRBS 3.1.1, DIS 3.30.0 | `accounting/service/AccountingEngine.java`, V31 |
| `organization` | `org_employee` master and headcount report | DIS 3.30.1-3.30.2 | new `organization/domain/Employee.java`, V31 |
| `report` | Report batch, column filters, print options | FRBS 2.4.x, ACSL 2.3.x | `report/core/ReportService.java`, `report/render/**`, V32 |
| `receivables` | XLSX / ODS statement import with layouts; `CHECK_NO_AND_AMOUNT` rule; hook "cheque presented" for DIS 3.26.1 | FRBS 3.3.1-3.3.2 | `receivables/service/BankStatementParser.java`, `AutoMatcher.java`, V551 |
| `payables` | Bank account active / inactive; cheque-book edit before use; DCTF notification format; `BankAccountQueryService` for disbursement | DIS 2.23.2, 2.24.2, 2.16.1 | `payables/domain/BankAccount.java`, `ChequeBook.java`, `NotificationFormat.java`, `PaymentNotificationFormatter.java`, V502 |
| `subledger` | `AgeingSlots.MAX_SLOTS` 5 -> 8 | ACSL 2.14.3 | `subledger/service/AgeingSlots.java` |
| `party` | Types EMPLOYEE, GOVERNMENT, OTHER_PAYEE | DIS 2.2.2 | `party/domain/PartyType.java`, V33 |
| `nbadmin` | Access request RETURNED | BASAU 2.4.1, 2.6.x | `nbadmin/domain/AccessRequestStatus.java`, V791 |
| `approval` | Bulk approve endpoint over `PendingApprovalSource` | FRBS 2.5.6, BASAU 2.5.3 | `approval/api/**` |
| `finreport` | Schedule engine, comparative formats with commentary | FRBS 3.2.0 | new `finreport/service/ScheduleEngine.java`, V652 |
| `tax` | Received-certificate register; new forms; BIR books; `TAX_CWT_CERT_RECEIVED` event | DIS 2.11, FRBS 3.2.0 | `tax/**`, V702-V703 |

### 12.2 Broking and Operations modules

| Module | Change | Why | Contract |
|---|---|---|---|
| `opsledger` (built) | `DisbursementRequest.Spec` gains `rfpNo`, `payeeClass`, `disbursementType`, `attachmentRefs`, `rootInvoiceNo`, `accountingRefs` (open-item / event refs to settle), `straightToApproval`; `Status` gains `CANCELLED`; `Type` gains SUPPLIER, GOVERNMENT, OTHER_BANK_UNIT, EMPLOYEE, CASH_ADVANCE, SERVICE_FEE, OTHER; `DisbursementStatusChanged` gains `dvStatus` and `instrumentStatus`; `QueueDisbursementGateway` becomes `@ConditionalOnMissingBean`; new ports of section 2.2; `ops_invoice.root_invoice_no` and Invoice 360 family view; search by assured, inception, AO; movement type CORRECTION | DIS 3.25.0, 2.20.0, 3.27.2, ACSL 2.5.x | Old `Spec` constructor kept as an overload (defaults) so remittance compiles unchanged; V765 |
| `remittance` (built) | `DisbursementFeedback` handles `CANCELLED` (reverse `OPS_REMITTANCE` for the batch with negative amounts, lines back to extractable, invoices unlocked, statuses back to PAID/not remitted) and `PAID` (batch "disbursed"); **CPC2** per batch line from `cat_incentive_criteria` with event `OPS_REMIT_CPC2`; **early incentive SI** through booking `ServiceInvoiceService` (type EARLY_INCENTIVE, 2% WTAX) before the incentive OR; **deductions** (`rem_deduction`) confirmed by ACSL and consumed by the next batch of the insurer, capped at net due, event `OPS_REMIT_DEDUCTION`; e-mail of the schedule on DV confirmation reuses `ScheduleDispatch` | DIS 2.20.0, 3.29.x, ACSL 2.9.2, DIS 2.7.12 | `remittance/service/DisbursementFeedback.java`, `BatchPosting.java`, `RemittanceAmounts`, V772 |
| `booking` (built) | `root_invoice_no` (= own invoice no. for originals; the root of the parent chain for endorsements / cancellations), exposed in `InvoiceBooked`; SI trigger ON_INCENTIVE and type EARLY_INCENTIVE with WTAX; manual issue of that type refused | DIS 3.27.2, 3.29.1, ACSL 2.16.0 | `booking/domain/BookedInvoice.java`, `booking/service/ServiceInvoiceService.java`, V872 |
| `adjustment` (built) | Carries `root_invoice_no` on requests and postings; its AR Insurer set-up becomes the source of `rem_deduction` candidates | ACSL 2.9.2, 2.16.0 | read only from opsledger; no migration |
| `crm` (built) | `crm_client_payout_account` written by `payrequest` on approval (port `ClientPayoutAccounts` in crm) | MKT 2.25.x | V802 |
| `catalog` (being extended by BRD-3) | `cat_incentive_criteria` read by remittance for CPC2 (packaged Fire / Motor) | DIS 3.29.2 | Contract ask to BRD-3 P1-A: `IncentiveCriteriaService.rate(code, product, insurer, date)` |
| `cashiering` (planned, O1-A) | Implements `RefundValidationSource` (reinstated to unapplied, new AR no.) and `PaymentReversalRequester`; the REFUND disposition creates / links a `payrequest` RRF instead of pushing to Disbursement directly; client 2307 batch release follows the DV status (Operations row 4); report of unapplied payments visible to Marketing processors | MKT 1.7.0, 1.11.0, ACSL 2.6.x, OQ15, OQ16 | Contract asks to O1-A (section 14) |
| `commission` (planned, O1-D) | `cmr_certificate_submission` uses the `tax_certificate_received` register; Motor Mania pass-on uses the extended `Spec` (type PASS_ON unchanged) | DIS 2.11, OQ41 | Contract ask to O1-D |
| Disbursement queue (built in opsledger) | Superseded by `disbursement`: the screen `/operations/disbursements` is hidden when the module is active; existing queue rows migrate once into `dsb_request` (status mapping section 7.2) | OQ02 | Migration handled in V891 (insert-select) |

## 13. Integrations to park (seam only)

| Item | BRD | Seam built now | Question |
|---|---|---|---|
| BDO Business Online Banking (funding, online-banking payments) | DIS 2.17, 3.26.7 | `BankChannelPort` with manual confirmation / BOB report upload; external link | AQ09, AQ10 |
| TPD / ACA (Direct Credit Transaction File) | DIS 2.16.1, 3.26.4 | DCTF file generated; credited .txt upload | AQ09 |
| Branch processing (ATD, MC / DD, CT / TT) | DIS 2.7.7-2.7.9 | Forms, e-mail through messaging, status confirmation by users | AQ09 |
| Bank deposited-checks file | DIS 2.22.0, 3.26.1 | Upload handler with layout table; bank reconciliation hook | AQ09 |
| Bank statement files for reconciliation | FRBS 3.3.1 | XLSX / ODS import with layouts | AQ08 |
| Insurer SOA | ACSL 2.2.1, 2.4.0 | `InsurerFileInbox` upload with layout per insurer | AQ21 |
| Payroll (HDMF, SSS, PhilHealth, 1601-C, 1604-C) | Appendix A VII | none (OUT) | AQ06 |
| BDO Unibank GARD submission | Appendix A II | Report exports only | AQ31 |
| Payee migration from the current system | DIS 2.2.8 | Migration handler with reconciliation report | AQ11 |
| eFPS / eBIRForms / CAS books | Appendix A VII | Export files per form (as the existing relief CSVs) | AQ07 |

## 14. Build-wave plan

Prerequisites: Operations O0 is built (opsledger, queue, ports). Cashiering (O1-A) and Commission (O1-D) may still be in
progress: BRD-5 only needs the ports they implement, with default adapters until they merge.

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **A0** (1 agent, short) | Foundation | V890 (all permissions, roles, LOV types, workflows, parameters, alerts); `opsledger` contract (Spec / Status / Type extensions, ports of section 2.2 with default adapters, `root_invoice_no` column and feed, V765); demo accounts / rules / users (V999); navigation entries, help registry, `application.yml` crons, Developer Guide ranges; empty module skeletons with `package-info.java` for `disbursement`, `payrequest`, `acsl`, `frbs` | `security/domain/Permission.java`, `opsledger/**`, `V765`, `V890`, `V999`, `frontend/src/navigation/modules.ts`, `help/helpContent.ts`, `application.yml`, `docs/development/DEVELOPER_GUIDE.md` | Everything compiles; remittance tests green with the extended Spec; `mvn verify` green |
| **A1-GL** | GL platform | Section 12.1 rows coa, journal, period / closing, currency, accounting (cost-centre rules), organization (employees), report, receivables, subledger, party, nbadmin, approval | those packages; V28-V33, V402, V551, V791; `frontend/src/features/{gl,closing,setup,reports}/**` | FRBS rows of sections B-F pass; chart upload demo file loads |
| **A1-DSB** | Disbursement | `disbursement` complete; payables changes; V999 masters; demo runner | `disbursement/**`, `payables/**`, V891-V893, V502, V999, `features/disbursement/**` | Remittance batch -> DV auto in FOR_APPROVAL -> approve (posting) -> check print -> negotiated upload; refund DV; cancellation restores the remittance batch; EOD outputs |
| **A1-PRQ** | Requests + ACSL | `payrequest` and `acsl`; crm payout accounts | `payrequest/**`, `acsl/**`, `crm/**` (payout only), V894-V897, V802, `features/payrequest/**`, `features/acsl/**` | RRF with validation -> approval -> DV; SOA upload -> recon report; correction case -> review -> approve -> posted with open items and ops movement |
| **A1-FRBS** | Reports and tax | `frbs`, `finreport` schedule engine, `tax` additions, service fee | `frbs/**`, `finreport/**`, `tax/**`, V652, V702-V703, V898-V899, `features/frbs/**`, `features/tax/**` | Schedule engine runs the seeded definitions; service-fee run -> payout request -> tags; new BIR outputs |
| **A1-OPSX** | Operations changes | remittance (cancel feedback, CPC2, SI, deductions), booking (root invoice, SI trigger) | `remittance/**`, `booking/**` (listed files), V772, V872 | Early-incentive SI with 2% WTAX; CPC2 lines; deduction consumed and capped |
| **A2** (1 agent) | Integration | E2E: booking -> receipt -> remittance -> DV -> negotiated -> SOA recon -> ACSL correction -> month-end close; refund RRF -> DV -> cancellation; module guides `docs/modules/DISBURSEMENT.md`, `ACSL.md`, updates to `OPERATIONS.md`, `PAYABLES_AND_CASH.md`, `PLANNING_AND_CLOSING.md`; workbook refresh | tests, docs, demo | Full `mvn verify` / `npm run verify`; walkthrough |

Rules for parallel work:
- One Flyway set per agent (table of section 4); no edits to another agent's packages.
- Shared files (Permission enum, navigation, help registry, `application.yml`, V890) are edited **only in A0**.
- Each module has its own `*ApiIT` smoke class.
- Ports have default adapters (`@ConditionalOnMissingBean`), so A1 agents test without each other. `payrequest` tests use
  the queue adapter until `disbursement` merges; `acsl` uses the default `PaymentReversalRequester`.
- A1-OPSX coordinates with A1-DSB on the `DisbursementStatusChanged` values defined by A0 only.
- Contract asks to the Operations agents O1-A (cashiering) and O1-D (commission) are recorded in section 12.2 and must be
  agreed before A1 starts; BRD-5 agents do not edit `cashiering/**` or `commission/**`.

## 15. What depends on information BDOI has not given (build the seam, park the content)

| Topic | Requirements | Built now | Parked content | Q |
|---|---|---|---|---|
| Real chart and entries | FRBS 2.3.x, DIS 2.7.6, OQ07 | Upload, numbering, events with demo rules | BDOI chart and rules | AQ01, AQ02 |
| Rates | FRBS 2.2.0, 3.5.0 | CLOSING input, BOOK copy job (parameter) | Rate source, daily vs monthly | AQ03 |
| Close timing | FRBS 2.6.x, 3.4.x | Schedules and cut-off | Agreed times; reopen policy | AQ04 |
| Report layouts | FRBS 3.2.0 | Schedule engine with draft definitions | GARD / subsidiaries / Mancom layouts; non-GL data | AQ05 |
| BIR forms and books | Appendix A VII | Worksheets / exports | Formats, 2550-M | AQ07 |
| Bank files and channels | DIS 2.16, 2.22, 3.26 | Layout tables, uploads, DCTF per Appendix B | Full specs | AQ08, AQ09 |
| Account funding rules | DIS 2.17 | Workflow with dual approval | Accounts, limits | AQ10 |
| Payee migration | DIS 2.2.8 | Handler | File | AQ11 |
| DV / check / form layouts | DIS 2.7.5, 2.16 | Draft templates | Layouts, signatories | AQ13, AQ14 |
| Refund / CA forms | MKT 1.10.0 | Appendix D fields | Mandatory fields, approvers | AQ18 |
| Service fee | FRBS 2.10 | Rule table and runs | Rates, recipients, liquidation | AQ20 |
| Insurer SOA formats | ACSL 2.4.0 | Layout per insurer | Samples | AQ21 |
| CPC2 / early incentive values | DIS 3.29.x | Engine and SI | Percentages, SI series | AQ24, AQ25 |
| Cost-centre rules | FRBS 3.1.1 | Rule table | Rules | AQ26 |
| Access matrix | all | Roles of section 8 | Confirmation | AQ28 |

## 16. Risks

1. **Scope of the report pack.** 138 Appendix A reports with titles only. Mitigation: the schedule engine turns most of
   them into configuration; layouts are confirmed in batches with FRBS (AQ05); non-GL data is excluded until sourced.
2. **Editable proforma entries weaken rule governance.** Mitigation: lines start from rules, edits are validated, marked
   and shown to the approver; a report lists edited DVs.
3. **Disbursement replaces a queue already used by remittance.** Mitigation: the port and the event stay; statuses map one
   to one; the queue adapter remains for tests; existing queue rows migrate.
4. **Cancellation after approval touches several modules.** Mitigation: one `CANCELLED` event with source references;
   each source restores itself in its own transaction (`@TransactionalEventListener` after commit) and records a
   regularisation result that feeds `DSB-UNREGULARIZED`.
5. **Dependence on cashiering and commission still in progress.** Mitigation: ports with default adapters; contract asks
   agreed before A1.
6. **The demo range is full after V999.** Mitigation: convention for V1000+ written into the Developer Guide by A0
   (section 4).

## 17. A0 foundation: as built

What the A0 wave built (together with the Collections foundation C0, because both edit the same shared files), and
where it differs from or details the sections above. The A1 waves compile only against this.

- **Migrations.**
  - `V765__ops_gateway_v2_root_invoice.sql`: new columns of `ops_disbursement_request` (`rfp_no`, `payee_class`,
    `disbursement_type`, `root_invoice_no`, `attachment_refs`, `accounting_refs`, `straight_to_approval`, `dv_status`,
    `instrument_status`, `cancelled_at`, `cancel_reason`), the new types and status CANCELLED;
    `ops_invoice.root_invoice_no` (not null, back-filled along the parent chain, indexed); movement type CORRECTION.
  - `V890__acct_foundation.sql`: roles and grants of section 8.2 (every new role also has WORK_VIEW, ATTACHMENT_VIEW,
    REPORT_VIEW, DASHBOARD_VIEW; the Disbursement, request and ACSL roles have OPS_VIEW, OPS_REPORT_VIEW, CLIENT_VIEW
    and ATTACHMENT_MANAGE; MKT_AO can raise and MKT_TL review refund requests; COMPTROLLERSHIP, SYSADMIN and AUDITOR
    read); `sec_permission_action` rows for the areas `DISBURSEMENT`, `PAYMENT_REQUESTS` and `ACSL` (the FRBS / GL
    permissions stay unclassified like the finance modules); the LOV types of section 9 with proposed values; the
    workflows of section 7; **the accounting event types** of section 6; parameters and alerts of section 9.
  - `db/demo/V999__demo_acct_chart_rules_users.sql`: the renames of section 3 (1210, 1211, 1225, 2205, 2210), the new
    accounts 1610, 1611, 1612, 2217, 2240, 2241, 2250, 2260, 4131, 5614, the demo rules of every event type below and the
    demo users of section 8.2 (`glofficer`, `gltl`, `glhead`, `disbtl`, `disbtl2`, `disbappr`, `disbappr2`, `mktao`,
    `mktrev`, `mktappr`, `hrappr`, `acsl`, `acsltl`, `acslhead`).
- **Event types are seeded in V890, not in V772 / V893 / V899.** The demo rules must be in V999 (A0), so the event
  types have to exist before it. A1 agents publish these events and must not insert them again:

  | Event | Components (amount keys) | Account roles the publisher supplies |
  |---|---|---|
  | `DISB_VOUCHER` | the gross in the component of the DV type: `REMITTANCE`, `REFUND`, `REFUND_FROM_INSURER`, `SUPPLIER`, `GOVERNMENT`, `OTHER_BANK_UNIT`, `EMPLOYEE`, `CASH_ADVANCE`, `SERVICE_FEE`, `PASS_ON`, `STALE_REISSUE`, `OTHER`; then `PAID` (net) and `EWT` | `PAY_ACCOUNT` (paying bank, or 2241 for checks when `DISB_CHECK_CLEARING` = ON), `EXPENSE` for OTHER |
  | `DISB_CHECK_NEGOTIATED` | `AMOUNT` | `BANK` |
  | `DISB_CHECK_STALE` | `AMOUNT` (party = payee) | - |
  | `DISB_FUND_TRANSFER` | `AMOUNT` | `TARGET_BANK`, `SOURCE_BANK` |
  | `TAX_CWT_CERT_RECEIVED` | `COMMISSION_CWT`, `INCENTIVE_CWT` | - |
  | `OPS_REMIT_CPC2` | `GROSS` (party = insurer), `CPC2_INCOME`, `OUTPUT_VAT` | - |
  | `OPS_REMIT_DEDUCTION` | `AMOUNT` (party = insurer) | - |
  | `FRBS_SERVICE_FEE_ACCRUE` | `AMOUNT` (cost centre required on 5614) | - |
  | `PRQ_CA_LIQUIDATION` | `PER_DIEM`, `REPRESENTATION`, `TRANSPORT`, `LODGING`, `OTHER`, `CASH_RETURNED`, `SHORTAGE`, `ADVANCE` | `PER_DIEM`, `REPRESENTATION`, `TRANSPORT`, `LODGING`, `OTHER`, `CASH` |

  One event type per DV with the type as the component keeps "GL accounts are never chosen in code": the account of
  each DV type is a rule line. `ACSL_CORRECTION` gets no event type: a correction posts as a system journal with the
  lines of the correction (row 18).
- **Not done in V999, left to the owners:** re-parenting 4130 under 4700 (4131 is created with report group "Other
  Income" under 4000 like 4130; the statements map by report group until FRBS uploads the real chart, AQ01); the USD
  accounts 1213 / 1221 / 2212 (only if BDOI splits by currency, AQ01); re-pointing the remittance and OR rules to
  `WTAX_COMMISSION` / `WTAX_INCENTIVE` (A1-OPSX changes the events); the BDOI value of `AGEING_BUCKETS`
  (30,90,180,365,730) waits for `AgeingSlots.MAX_SLOTS` = 8 (A1-GL).
- **Demo data of the A1 waves.** V999 is taken by A0, and Flyway cannot hold two V999 files. A1-DSB seeds its demo
  masters (payees, bank-account statuses, cheque books, employees, cost-centre rules, layouts) with its Java demo
  runner, like the demo transactions of every A1 module (section 4 "demo takes V999 ... demo runners").
- **`opsledger` contract (section 12.2).**
  - `DisbursementRequest.Spec` has 16 components (the 9 of BRD-2, then `rfpNo`, `payeeClass`, `disbursementType`,
    `attachmentRefs`, `rootInvoiceNo`, `accountingRefs`, `straightToApproval`); the 9-argument constructor is kept, so
    remittance, cashiering and commission compile unchanged; `spec.routed(rfp, payeeClass, type, root, straight)` and
    `spec.withReferences(documents, settles)` build the extended form. `Type` gains SUPPLIER, GOVERNMENT,
    OTHER_BANK_UNIT, EMPLOYEE, CASH_ADVANCE, SERVICE_FEE, OTHER; `Status` gains CANCELLED.
  - `DisbursementStatusChanged` gains `dvStatus` and `instrumentStatus` (8-argument constructor kept). The queue
    publishes it on cancel (`DisbursementQueueService.cancel(id, reason)`, `POST /api/v1/ops/disbursements/{id}/cancel`)
    and on `track(id, dvStatus, instrumentStatus)`, which records the DV stage and instrument status without changing
    the gateway status; the `reason` of a CANCELLED event is the cancellation reason.
  - Ports with `@ConditionalOnMissingBean` defaults in `OpsPortDefaults`: `RefundValidationSource` (one bean per
    validator, `ACSL` / `CASHIERING`; default `HandoffRefundValidationSource` answers `ANY`), the router
    `opsledger.service.RefundValidations.open(request)` that payrequest calls (it hands over the validator whose module
    is missing), `PaymentReversalRequester` (default `HandoffPaymentReversalRequester`, team CASH_APPLY),
    `InvoiceCorrectionSink` (implemented by `LedgerInvoiceCorrectionSink`: signed CORRECTION movements, idempotent on
    source module / reference / invoice). Results come back as `RefundValidationCompleted` and
    `PaymentReversalCompleted` in `OpsLedgerEvents`.
  - `ops_invoice.root_invoice_no`: set by the booking feed (the parent's root, else the invoice itself), exposed on the
    invoice DTO (`keys.rootInvoiceNo`), `InvoiceLedgerQueryService.family(invoiceNo)` and
    `GET /api/v1/ops/invoices/{no}/family`. When A1-OPSX adds `bkg_invoice.root_invoice_no`, both agree by
    construction. The Invoice 360 family view and the search by assured, inception and AO stay with A1-OPSX.
  - `ReportMetadata.disbursement(...)`, `acsl(...)`, `frbs(...)` and `ReportCategory` DISBURSEMENT, PAYMENT_REQUESTS,
    ACSL, FRBS for the report catalogue.
- **Navigation.** Group **Finance**, after Commission Receivables: Disbursement (`/disbursement`, module / help id
  `disbursement`, DISB_VIEW), Refund & Cash Advance Requests (`/payment-requests`, `payrequest`, PRQ_VIEW), ACSL
  (`/acsl`, `acsl`, ACSL_VIEW), Accounting Reports (`/frbs`, `frbs`, FRBS_REPORT_VIEW). The requests are in Finance,
  not in Operations as section 11 proposed (integration decision of this wave). Each is a landing screen until its A1
  wave adds the real screens in its own `features/<module>/module.ts` and `help.ts`.
- **Jobs.** The crons of section 9 are in `application.yml` (`journal-auto-reversal-cron`, `gl-period-close-cron`,
  `broking-books-close-cron`, `book-rate-from-closing-cron`, `disb-check-stale-cron`, `disb-eod-confirmation-cron`,
  `disb-eod-reports-cron`, `acsl-gl-sl-recon-cron`) and `docs/operations/CONFIGURATION.md`.
- **Module skeletons.** `disbursement`, `payrequest`, `acsl` and `frbs` hold only `package-info.java`.
- **Flyway check.** Every version of section 4 is still free except `V791`, which Product Maintenance used for the
  role-permission change requests (`V791__nbadmin_role_permission_requests.sql`): A1-GL puts the access-request
  RETURNED status in `V792`. A0 used V765, V890 and demo V999; nothing else of the BRD-5 ranges.

### A1-GL as built

GL platform wave (section 12.1 rows coa, journal, period / closing, currency, accounting, organization, report,
receivables, subledger, party, nbadmin, approval). Every change is additive; existing callers and tests are unchanged.

**Migrations** (only V1-V27 tables are referenced by V28-V33, so they are safe before every module on a fresh
database):

| Version | Content |
|---|---|
| `V28__coa_upload_numbering_short_code.sql` | `coa_account.negative_balance_policy` (ALLOW / WARN / BLOCK); short code unique per company (case-insensitive, later duplicates cleared first); `coa_numbering` |
| `V29__journal_assignment_reversal_links.sql` | `jnl_batch.assigned_to / assigned_by / assigned_at`, `reverse_on`, `corrects_batch_id`, `related_invoice_no`, `root_invoice_no` |
| `V30__period_module_lock_close_schedule.sql` | `acc_period_module_lock`, `acc_period_close_schedule`; parameter `BROKING_SOURCE_MODULES` |
| `V31__cost_center_rules_and_employees.sql` | `acc_cost_center_rule`, `org_employee` |
| `V32__report_batch_print_options.sql` | `report_batch`, `report_batch_item` (print options on the batch; `report_run` is created later by V762, so the options of a single export go into its parameter echo) |
| `V33__party_payee_types.sql` | party types EMPLOYEE, GOVERNMENT, OTHER_PAYEE (sub-ledger VENDOR); check constraint on `pty_party.party_type` |
| `V402__closing_year_end_verification.sql` | `yec_year_end_close.nominal_balance`, `tb_difference`, `verified`, `verified_at` (the parameters `CLOSE_ONLY_PREVIOUS_MONTH` and `YEAR_END_CLOSE_DEADLINE` were already seeded by V890) |
| `V551__bank_statement_layouts_rules.sql` | `brs_statement_layout`, `brs_match_rule` (by GL bank account code) |
| `V792__access_request_returned.sql` | access request status RETURNED, `returned_count` |

**By requirement.**

| BR | As built |
|---|---|
| FRBS 2.2.0, 3.6.0 | `currency.service.RevaluationRateService`: the monthly revaluation rate is the CLOSING rate of the month end (`POST /api/v1/currencies/revaluation-rates`, `REVALUATION_RATE_MAINTAIN`; `GET ...?year=`). Job `BOOK_RATE_FROM_CLOSING` (`book-rate-from-closing-cron`) copies the rates of the month that ends as the BOOK rates of the next month when `OPS_BOOK_RATE_SOURCE = CLOSING_PREV_MONTH`, never overwriting a BOOK rate; `POST .../revaluation-rates/{yyyy-MM}/copy-to-book` does it on demand. Screen: Setup > Currencies & Rates, card "Monthly revaluation rates" |
| FRBS 2.3.1 | Bulk handler `COA_ACCOUNTS` (`coa.service.CoaUploadHandler`) on the bulk framework, exposed under the chart's own permission `COA_UPLOAD` by `/api/v1/coa/uploads` (template, upload, rows, commit, cancel, report), because FRBS_TL has no BULK_PROCESS. A parent is an existing account or an earlier row of the file; every account is created PENDING_AUTHORIZATION. Sample `docs/samples/coa_upload_sample.xlsx` (8 accounts under new groups 1900 / 2900) loads cleanly (`GlPlatformApiIT`). Screen: General Ledger > Chart Upload |
| FRBS 2.3.2 | `coa_numbering` per parent (separator and width); a blank code on create or upload takes the next free number (`CoaNumberingService.nextCode`, existing codes with the prefix are never reused); `GET /coa/accounts/next-code`, `GET/PUT /coa/numbering` |
| FRBS 2.3.3 | Short code unique (`SHORT_CODE_TAKEN`, `coa.service.ShortCodes`), searched with code and name; `GET /coa/accounts/lookup?key=` by code or short code; journal lines accept a short code in place of the account code (`JournalLineResolver`) |
| FRBS 2.5.1 | `POST /journals/assign` (`JOURNAL_ASSIGN`; the assignee must hold JOURNAL_AUTHORIZE and not be the submitter), `GET /journals/assignees`, filter `assignedTo` on the journal search; Journals screen "Assign" and "Assigned to me" |
| FRBS 2.5.4, 2.5.5, 2.8.4, 3.6.0b | `journal.service.NegativeBalanceCheck` (manual journals only): BLOCK accounts refuse the journal in `JournalValidator` (submit and approve), WARN accounts give warnings (`GET /journals/{id}/warnings`) shown in the confirmation dialogs; natural side from the account class |
| FRBS 2.5.6, BASAU 2.5.3 | `POST /journals/bulk-approve` (each journal in its own transaction with every control; partial success report) and the generic `POST /api/v1/approvals/bulk-approve` over the new port `approval.service.BulkApprovalAction`, implemented for GL journals and access requests (new-user requests are refused in bulk because their temporary password is shown once) |
| FRBS 2.5.7 | "Reject" is labelled "Return to Maker" on the journal screen |
| FRBS 2.5.10, 2.8.3 | Confirmation dialog with totals, lines, dates and warnings before submit and before authorize (`features/gl/ConfirmPostingDialog`) |
| FRBS 2.8.1 | `reverse_on` on manual / adjustment / accrual journals (must follow the value date, editable while DRAFT / REJECTED); job `JOURNAL_AUTO_REVERSAL` posts a REVERSAL system journal (source `AUTOREV`, key `JV:<batch>:AUTOREV`, `reversal_of_id`) on that date, one transaction per journal. Manual journals record no sub-ledger open items today, so there is none to reverse |
| FRBS 2.6.0, 2.6.1 | `closing.service.PeriodCloseScheduleService`: schedule (proposal: 2nd banking day of the next month, 17:00 Manila, company holidays skipped), withdraw, close now; job `GL_PERIOD_CLOSE` runs due closes: checklist, then `PeriodService.close`; a refusal is recorded on the schedule and raises `GL_CLOSE_FAILED`. With `CLOSE_ONLY_PREVIOUS_MONTH` only the month before the close date is accepted. Screen: Planning & Closing > GL Close & Cut-Off |
| FRBS 2.7.0 | Alert check `YEAR_END_CLOSE_DUE` (`YearEndCloseDueCheck`, daily alert job): previous fiscal year open within the threshold days (15) before `YEAR_END_CLOSE_DEADLINE` |
| FRBS 2.7.1 | `YearEndService.close` verifies the close (nominal balance and TB difference as of the year end) and stores it; `POST /closing/year-end/verify`; shown on the year-end panel with "Verify Again" |
| FRBS 3.4.0, 3.4.1 | `acc_period_module_lock` (group `BROKING`); `PeriodService.requirePostingPeriod(company, date, systemOrAdjustment, module)` overload; the accounting engine refuses events of the source modules in `BROKING_SOURCE_MODULES` into a period whose broking books are closed (`BOOKS_CLOSED`), so booking, Operations, cashiering, remittance, adjustment, commission and disbursement are cut off without changing their code; GL adjustments still post. Job `BROKING_BOOKS_CLOSE` closes them on the last day of the month; `/closing/broking-books` (status, pending, close, reopen with reason). Pending items come from the new port `closing.service.BrokingCutoffCheck` (no implementation yet) |
| FRBS 2.4.4, 2.4.5, 2.4.7, 2.4.9 | Column filter row in the report viewer; exports take `filter=column:text` (detail rows only, note on the file), `paper`, `orientation`, `fitToWidth` (PDF); report batches `POST /api/v1/reports/batches` (ZIP of any format or one merged PDF, per-report permissions, failures listed, status COMPLETED / PARTIAL / FAILED), `GET /reports/batches[/{id}[/file]]` (creator only). Batches run synchronously in the request; one transaction per report. Screen: Report Centre > Report Batch |
| FRBS 3.1.1, DIS 3.30.0 | `acc_cost_center_rule` (`/api/v1/accounting/cost-center-rules`, ACCOUNTING_RULE_MANAGE or MASTER_MAINTAIN): the engine fills the lines of cost-centre-required accounts from the first matching rule; still missing: `COST_CENTER_MISSING` (event FAILED in the register, alert raised in its own transaction). Screen: Setup > Cost-Centre Rules |
| FRBS 3.3.1, 3.3.2 | `brs_statement_layout` per bank account and `POST /receivables/bank-rec/statements/file` (xlsx / ods / csv via the bulk reader, mapped to the standard statement, imported, then auto-matched); rule `CHECK_NO_AND_AMOUNT` (`AutoMatcher` first pass: same cheque number in the reference or narration, same amount, any date). Screen: Setup > Bank Statement Layouts |
| DIS 2.2.2 | Party types EMPLOYEE, GOVERNMENT, OTHER_PAYEE |
| DIS 3.30.1, 3.30.2 | `org_employee` (`/api/v1/organization/employees`, EMPLOYEE_MAINTAIN), report `ORG-HEADCOUNT-CC` (`ReportMetadata.frbs`). Screen: Setup > Employees |
| ACSL 2.9.1, 2.16.0 | `SystemJournalRequest.correcting(corrects, relatedInvoice, rootInvoice)` stores the links on the journal (for the ACSL correction entries of A1-PRQ) |
| ACSL 2.14.3 | `AgeingSlots.MAX_SLOTS` = 8 (the BDOI buckets 30, 90, 180, 365, 730 fit; setting `AGEING_BUCKETS` to them is a configuration step, it changes every ageing report's default) |
| BASAU 2.4.1, 2.6.0-2.6.3 | Access request RETURNED (`nbadmin.service.AccessRequestReturnService`): `POST /nbadmin/access-requests/{id}/return` (remarks mandatory, four eyes, requester notified) and `/{id}/resubmit` (requester, new justification) |

**Contracts for other modules.** `PeriodService.requirePostingPeriod(..., module)` and `findPeriod`;
`PeriodModuleLockService.requireOpen(company, date, module)`; port `closing.service.BrokingCutoffCheck` (broking
modules list their pending items); port `approval.service.BulkApprovalAction` (bulk approval of inbox items);
`SystemJournalRequest.correcting(...)`; `CostCenterRuleService` (applied by the engine, nothing to call);
`ReportService.export(code, params, format, ExportOptions)`, `runForExport`, `render`; `EmployeeService.active`.

**Jobs.** `JOURNAL_AUTO_REVERSAL`, `GL_PERIOD_CLOSE`, `BROKING_BOOKS_CLOSE`, `BOOK_RATE_FROM_CLOSING` on their
pre-registered crons (`...-cron:-` defaults).

**Tests.** `GlPlatformApiIT` (HTTP: read endpoints, chart upload of the sample file, numbering, revaluation rates,
cost-centre rules, employees, report batch, export options, closing controls, headcount report),
`JournalFrbsControlsIT`, `CloseControlsIT`, `CostCenterRuleIT`, `StatementFileImportIT`, `AccessRequestReturnIT`,
`YearEndIT` (verification), unit tests `CoaNumberingTest`, `ExportOptionsTest`, `AutoMatcherChequeTest`,
`AgeingSlotsTest`; frontend `closeTimes`, `setupForms`, `reportOptions` tests.

**Parked / not done here.**
- Real chart, entries and cost-centre rules stay configuration (AQ01, AQ02, AQ26, OQ07); close times and reopen policy
  (AQ04); daily revaluation and the rate source (AQ03); bank file layouts (AQ08) are data of `brs_statement_layout`.
- The re-parenting of 4130 under 4700, the USD accounts 1213 / 1221 / 2212 and the BDOI `AGEING_BUCKETS` value are
  chart / parameter configuration, not migrations (AQ01).
- Grants that belong in a later version: FRBS_TL has no `BULK_PROCESS` and my Flyway ranges all run before V890 on a
  fresh database, so the chart upload got its own endpoints under `COA_UPLOAD` instead of a grant.
- Frontend of the access-request "Return" (screen owned by `features/brokingsetup`) and of the approval inbox bulk
  approve (screen owned by the approvals feature): the APIs are ready; the Journals screen has its own bulk posting.
- The broking modules do not implement `BrokingCutoffCheck` yet: the cut-off records "Nothing pending" until they do.

### A1-PRQ as built

What wave A1-PRQ built for `payrequest`, `acsl` and the `crm` payout accounts, and where it differs from or details
sections 5, 9-11 and 15. The fit/gap rows of `docs/requirements/BDOI_ACCT_BRD_SPEC.md` sections N-P are unchanged;
this table is their build status.

- **Migrations.** `V802__crm_client_payout_account.sql` (`crm_client_payout_account`: mode CTA / CHECK, payee name and
  its normalised key, BDO account no., source request, active; partial unique indexes on (client, account no.) and
  (client, payee key) for CHECK among active rows). `V894__payrequest.sql` (`prq_request`, `prq_request_line` with a
  unique index on the AR no. of live lines, `prq_validation` unique per (request, round, line, validator) and per
  (validator, source reference), `prq_liquidation`, `prq_liquidation_line`, the configuration table
  `prq_liquidation_account`, LOV types `PRQ_PAYMENT_MODE` and `PRQ_RFP_TYPE`, notification event
  `PRQ_REQUEST_STATUS`). `V895__payrequest_templates.sql` (document templates `PRQ_RRF`, `PRQ_RFP`,
  `PRQ_LIQUIDATION`). `V896__acsl.sql` (`acsl_case`, `acsl_correction`, `acsl_correction_line`, `acsl_soa_layout`
  seeded with the standard layout `*`, `acsl_soa_upload` unique per company / insurer / file hash, `acsl_soa_line`,
  `acsl_recon_run`, `acsl_recon_result`, `acsl_glsl_control`, `acsl_glsl_run`, `acsl_glsl_recon`).
  `V897__acsl_events_reports.sql` (notification events `ACSL_CASE_STATUS`, `ACSL_GLSL_DIFFERENCE`; parameter
  `ACSL_SOA_MAX_ROWS`). No event type is inserted: the liquidation publishes `PRQ_CA_LIQUIDATION` of V890 and a
  correction posts a system journal (section 17).
- **Contracts.** `crm.service.ClientPayoutAccounts` (validate, record without duplicates, list, deactivate) is what
  payrequest calls on approval of a refund. `acsl.service.AcslRefundValidationSource` replaces the default
  `RefundValidationSource` for validator `ACSL` (it opens an ANALYSIS_REQUEST case; the result publishes
  `RefundValidationCompleted`). payrequest consumes `RefundValidationCompleted` and `DisbursementStatusChanged`; acsl
  consumes `PaymentReversalCompleted`. The check cancellation is handed off on port `DV_CANCELLATION` (team
  `DISB_APPROVE`) because the gateway has no cancel for a DV; A1-DSB can pick it up.

| BR ID | Status | How it is built |
|---|---|---|
| MKT 1.2.0-1.6.0 | Built | Requests Home `/payment-requests`: tabs by stage with counts, search (request, payee, reference, DV no.), filters by kind and date, bulk endorse / approve with per-item results; request page `/payment-requests/requests/:id` |
| MKT 1.8.0, 1.17.0 | Built | Generic return and cancel transitions of `PRQ_REFUND`, `PRQ_CASH_ADVANCE`, `PRQ_CHECK_CANCEL`; cancelling releases the AR lines |
| MKT 1.9.0 | Built | `POST /api/v1/payment-requests/requests/{id}/assign` through `WorkAssignmentService` (PRQ_ASSIGN) |
| MKT 1.10.0 | Built, seam parked | RRF lines and the RFP; PDF forms from the templates of V895; mandatory fields and approvers wait for AQ18 |
| MKT 1.11.0 | Built, seam parked | A cancelled-policy line opens one validation per line for `ACSL` (case) and `CASHIERING` (handoff default until C1-C / O1-A provide a source); both results decide validated / returned; manual result entry for a handed-off validator |
| MKT 1.12.0, 2.22.0 | Built | Attachments on the request; their ids travel to Disbursement in `Spec.withReferences` |
| MKT 1.14.0-1.16.3 | Built | Submit, endorse, approve (four eyes: no endorse or approve by the requester or the previous actor), HR approval of cash advances |
| MKT 1.18.0-1.18.1 | Built | Reports `PRQ-STATUS` and `PRQ-REGISTER` (category PAYMENT_REQUESTS) |
| MKT 1.19.0, 1.16.3 | Built, seam parked | Check cancellation of a disbursed CHECK / manager's check / demand draft with a DV, one live cancellation per request; approved requests are handed off on `DV_CANCELLATION` |
| MKT 1.20.0, 2.26.0 | Built | `DisbursementStatusChanged` moves the request to disbursed (PAID) or back to the preparer (RETURNED / CANCELLED, resent under a new reference `RRF.../n`); notification `PRQ_REQUEST_STATUS`; Disbursement tab |
| MKT 2.23.0 | Built | Unique live AR no. (`uq_prq_live_ar`) |
| MKT 2.24.0 | Built | `DisbursementGateway.send` on final approval (refund) or HR approval (cash advance) |
| MKT 2.25.0-2.25.1 | Built, seam parked | Payout accounts recorded on approval without duplicates; the account-number format is a 10-16 digit rule until AQ19 |
| Appendix D liquidation | Built, seam parked | Liquidation tab: fieldwork days, submit, return, post (`PRQ_CA_LIQUIDATION`, four eyes); the account of each expense role is the configuration screen `/payment-requests/liquidation-accounts` (AQ02 / OQ07) |
| ACSL 2.2.1, 2.4.0 | Built, seam parked | SOA upload (CSV, XLSX, ODS, TXT) per insurer and period, layout per insurer in `acsl_soa_layout` (standard layout until AQ21), upload log `ACSL-SOA-UPLOAD-LOG`; screen `/acsl/soa` |
| ACSL 2.13.0-2.13.1, 2.14.0-2.14.1 | Built | Reconciliation by invoice no. into Outstanding / For remittance / Remitted / Cancelled / Direct billed / Not found with premium and balance variances; report `ACSL-SOA-RECON` named "insurer_from_to"; reconcile again; screen `/acsl/soa/:id` |
| ACSL 2.2.0, 2.14.2 | Built | Report `ACSL-BOOKED-FIN-DETAILS` |
| ACSL 2.13.2 | Built, seam parked | Job `ACSL_GL_SL_RECON` (ManagedJob on `acsl-gl-sl-recon-cron`) and on demand, report `ACSL-GL-SL-RECON`, alert `ACSL_GLSL_DIFFERENCE`; control accounts and their sub-ledger are configuration (`/acsl/gl-sl`, OQ07). The period-end check provider is not built |
| ACSL 2.14.3-2.14.4 | Not built | Ageing and schedule reports need `AgeingSlots` with 8 slots (A1-GL, OQ43) |
| ACSL 2.5.0, 2.5.4-2.5.5 | Built | Cases `/acsl` (investigation, analysis request, AR refund application, payment reversal): assign, findings, result to the requester; cases of the invoice family |
| ACSL 2.6.0-2.6.1 | Built, seam parked | Payment reversal request from a case through `PaymentReversalRequester` (handoff default until Cashiering implements it); the result comes back as `PaymentReversalCompleted` |
| ACSL 2.6.2 | Built | Message to the Account Officer of the invoice |
| ACSL 2.7.0-2.12.2, 2.15.0 | Built | Correction entries `/acsl/corrections`: assign / re-assign, lines with party, invoice and ledger component, submit (balanced only), endorse, approve (four eyes) posting a system journal `ACS:<no>` in the journal type of the corrected batch, recording and matching open items, and moving the invoice components through `InvoiceCorrectionSink` |
| ACSL 2.9.1 | Built, seam parked | "Wrong account" proposal: reversal of the original line plus re-post to the right account, linked by invoice family and original batch / line; the `corrects_batch_id` column on journals waits for A1-GL (AQ22) |

- **Differences from the design.** The SOA upload reads the file with `BulkFileReader` in one request (row limit
  `ACSL_SOA_MAX_ROWS`) instead of a bulk handler, so the reconciliation runs once the whole file is loaded. Menu:
  Requests Home, New Refund Request, New Cash Advance, Cancel a Check, Liquidation Accounts; ACSL Cases, Correction
  Entries, Insurer SOA Reconciliation, GL-SL Reconciliation.
- **Parked (seams).** AQ18 forms and approvers; AQ19 account-number format; AQ21 insurer SOA layouts (table
  `acsl_soa_layout`); AQ02 / OQ07 liquidation and GL-SL control accounts (configuration tables); Cashiering validation
  and payment reversal via handoff until C1-C; DV cancellation via handoff; `corrects_batch_id` until A1-GL; ACSL
  ageing / schedule reports until `AgeingSlots` has 8 slots.
- **Tests.** `PayRequestFlowIT`, `RefundValidationIT`, `LiquidationAndReportsIT`, `PayRequestApiIT`,
  `PayRequestDomainTest`, `AcslSoaIT`, `AcslCorrectionIT`, `AcslApiIT`, `AcslDomainTest`; frontend
  `requestForm.test.ts`, `acsl.test.ts`.

### A1-DSB as built

What the A1-DSB wave built for `disbursement` (DIS 2.2-3.28) and where it differs from or details the sections above.

- **Migrations.** `V502__payables_bank_status_dctf.sql` (`pay_bank_account.status` / `requested_status`, DCTF
  notification format, cheque-book edit audit `edited_by` / `edited_at` / `previous_range`);
  `V891__disbursement_payees_requests.sql` (`dsb_payee`, `dsb_payee_account`, `dsb_payee_request`, `dsb_request`; the
  one-time insert-select copies the queue rows still SENT / ACKNOWLEDGED as NO_PAYEE requests);
  `V892__disbursement_vouchers_instruments.sql` (`dsb_voucher`, `dsb_voucher_line`, `dsb_instrument`,
  `dsb_instrument_event`, `dsb_voucher_tag`, `dsb_status_edit`, `dsb_eod_run`, `dsb_eod_output`,
  `dsb_funding_request`); `V893__disbursement_events_templates.sql`. The event types stay in V890 (section 17), so
  V893 holds only the document templates (`DSB_VOUCHER`, `DSB_ATD`, `DSB_ATD_EMAIL`, `DSB_MC_DD`, `DSB_CREDIT_TICKET`,
  `DSB_TT`, `DSB_CHECK`, `DSB_PAYMENT_ADVICE`) and the smallest shared additions the module needed: parameters
  `DISB_NO_PAYEE_ACTION` (HOLD), `DISB_CHECK_SERIES_WARNING` (20), `DISB_CHECK_CLEARING_ACCOUNT` (2241); LOV value
  `DISBURSEMENT_TYPE` / `STALE_REISSUE`; workflow transition `DISB_PAYEE` ACTIVE -`amend`-> FOR_AUTHORIZATION.
- **No V999 masters.** V999 is A0's; the demo masters and storyline are in the Java runner
  `disbursement.demo.DisbursementDemoData` (`@Order(97)`, after the Operations runners, each step signed in through
  `DemoUsers.as`): payees by `disbtl` authorised by `disbappr`; a second remittance batch in review (only when
  Operations left more than one, so its storyline keeps a batch in review) approved and paid by check, otherwise an
  e-mailed remittance encoded and left for the approver; a refund by credit to account; a supplier check printed,
  released and negotiated; a supplier request left in process; the end of day; a funding through maker, verifier and
  two approvers.
- **Gateway.** `DisbursementGatewayAdapter` is the `@Primary` `DisbursementGateway`. It keeps the Operations queue record
  (DSQ numbers, `DisbursementStatusChanged` events) through `DisbursementQueueService` and creates the `dsb_request`;
  it never acknowledges at send. `GatewaySync` mirrors the DV back to the queue: every DV stage -> `track`, approval ->
  `assignDv` (DV_ASSIGNED), paid instrument -> `markPaid`, rejection / return -> return to source, cancellation ->
  `cancel(reason)` (CANCELLED, which A1-OPSX consumes to restore the remittance batch). Requests already moved on the
  queue screen get no DV.
- **Intake and automatic DV (DIS 3.25.x).** A request with a usable payee (party code first, then the only payee of
  that name) gets its DV at once through `VoucherFactory` (non-transactional, so a refused DV never marks the caller's
  transaction rollback-only); remittances and refunds route straight to the approver. Without a payee the request
  waits as NO_PAYEE with a NO_MATCH payee request and the alert `DISB_PAYEE_NO_MATCH`, and resumes on
  `PayeeService.PayeeAuthorized`; `DISB_NO_PAYEE_ACTION` = RETURN returns it at once instead.
- **Voucher.** Terms (mode, paying and payee account, EWT, purpose, value date, cost centre, expense account), proforma
  from `RuleResolver` / `JournalLineBuilder`, line edits marked EDITED, expense allocation, rebuild from rule. Approval
  posts through `AccountingEventPublisher` (rule entry) or `SystemJournalService` (edited entry); a posting failure keeps
  the DV for approval with posting status FAILED and the error (`DV_POSTING_FAILED`). Bulk approval is one transaction per DV. Cancelling an approved DV reverses its
  journal (REVERSED / REVERSAL_FAILED) after the workflow transition, cancels the instrument and returns the request.
  Cancelling a DV also closes the open Payment Requests check-cancellation hand-offs (port `DV_CANCELLATION`, team
  `DISB_APPROVE`) that name it (`CancellationHandoffs`).
- **Instruments.** One per approved DV; the life cycle per mode is `InstrumentLifecycle` (section 7.2). Checks take the
  next leaf of the paying account's active cheque book (alert `CHECK_SERIES_LOW` under `DISB_CHECK_SERIES_WARNING`);
  negotiated checks post `DISB_CHECK_NEGOTIATED`, the stale job posts `DISB_CHECK_STALE` and a re-issue creates a
  `STALE_REISSUE` request. Status edits go through `DISB_STATUS_EDIT` (team leader). Forms and vouchers are PDFs of
  the templates above; the ATD is e-mailed to the branch.
- **End of day.** `EodService.run(company, date)` freezes the approved DVs, writes the DCTF (`PaymentNotificationFormatter.dctf`:
  header `MMddyyyy` + name, 89-character detail: 12-digit account, 30 payee, 12 blanks, 20 reference, amount
  `000000000000.00`, upper case; no trailer until AQ09), the check batch, the forms and the six EOD reports; `confirm`
  sends the payment advices. Jobs (`ManagedJob` on the A0 crons): `DISB_CHECK_STALE`, `DISB_EOD_CONFIRMATION`,
  `DISB_EOD_REPORTS`.
- **Uploads** (bulk handlers): `DISB_REQUESTS`, `DISB_CHECKS_NEGOTIATED`, `DISB_CTA_CREDITED`, `DISB_BOB_APPROVED`,
  `DISB_PAYEE_MIGRATION`.
- **Reports** (`ReportMetadata.disbursement`): `DSB-MASTERLIST`, `DSB-UNRELEASED-CHECKS`, `DSB-ML-STALE`, `DSB-ATD`,
  `DSB-CASH-FLOW`, `DSB-CWT-COMMISSION`, `DSB-PAYEE`, `DSB-PAYEE-NOMATCH`, `DSB-UPLOAD-FALLOUT`, `DSB-UNREGULARIZED`,
  `DSB-EOD-REMIT`, `-REFUND`, `-SUPPLIER`, `-EMPLOYEE`, `-OTHER`, `-SUMMARY`.
- **API** (`/api/v1/disbursement`): `summary`; `requests` (list, get, encode, `/{id}/voucher`, `/return`, `/release`);
  `vouchers` (list, get, `terms`, `proforma`, `proforma/reset`, `allocation`, `submit`, `route`, `submit-for-approval`,
  `approve`, bulk `vouchers/approve`, `reject`, `cancel`, `document`, `tags/receipt`, `tags/cwt`,
  `instrument/print|document|release|email|debited|received|reissue|status-edits`); `status-edits` (list,
  `/{id}/approve`); `payees` (CRUD, `submit`, `authorize`, `deactivate`, `reactivate`, accounts), `payee-requests`;
  `eod/runs`, `eod/runs/{id}/confirm`, `eod/outputs/{id}`; `banks` (`status`, `authorize`, `cheque-books`),
  `cheque-books/{id}`; `funding` (CRUD, `submit`, `verify`, `approve`).
- **Screens** (`features/disbursement`): Disbursement Workbench, voucher record (details, entry, instrument, OR / AR
  and CWT, documents, e-mails), Encode Payment Request, Payees and payee record, Disbursement Uploads, Disbursement End
  of Day, Account Funding and funding record, Bank Accounts and Checks, Disbursement Reports.
- **Payables changes (additive).** `BankAccount.status` / `requestedStatus` (a status change waits for authorisation;
  `isActive()` = authorised and ACTIVE), `ChequeBook.editRange` (before the first leaf), `NotificationFormat.DCTF`;
  `BankAccountResponse` and `ChequeBookResponse` gain the new fields.
- **Tests changed outside the module.** `FlowInAndPortsIT` now expects `DisbursementGatewayAdapter` as the gateway.
- **Parked (seams built).** AQ09 bank channel layouts (DCTF trailer / totals, credited and deposited-check files, BOB
  report: the uploads take a minimal CSV); AQ10 funding accounts and limits (both approvers required, in order); AQ11
  payee migration file and the default no-payee behaviour; AQ12 request sources and upload columns; AQ13 DV number
  format and EWT on the DV; AQ14 check, voucher and form layouts and signatories (draft templates); AQ15 cancellation
  after release; AQ16 the received-certificate register behind `DSB-CWT-COMMISSION`; AQ17 which DVs need an OR / AR back
  (all approved DVs are unregularised until tagged); the remittance schedule attachment on the insurer check; open-item
  matching of the paid AP line (the DV posts through the rules only); hiding the Operations queue screen
  `/operations/disbursements` (shared navigation, left to the Operations owner).
