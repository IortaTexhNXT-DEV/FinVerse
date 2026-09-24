# BDOI Operations (BRD-2) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse.

Status: **for BDOI concurrence; build planned after the BRD-1 booking contracts (see the design, section 13).** The review workbook will be consolidated with the other BRDs. Build design: [`OPERATIONS_DESIGN.md`](../architecture/OPERATIONS_DESIGN.md).

## 1. Source documents

Source: `docs/source-documents/Operations.pdf` (132 pages).

| Pages | Document | Content |
|---|---|---|
| 1-7 | Operations Addendum 1, v1.0 18-Dec-2025, signed Jan-2026 | Updates RMTID.002 (exclusion only, no editing of financial data) and ADJID.014 (service invoice when income / commission is affected) |
| 8-15 | Operations BRD v1.0 (Jul-2025): executive summary, current vs envisioned process, key capabilities | Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation, Collection of Commission Receivables (Direct Payment) |
| 16-20 | General requirements | BRQID.001-006 |
| 21-46 | Cashiering | CSHID.001-027, MKTID.013, DBMID.001 |
| 47-76 | Remittance | RMTID.001-040, MKTID.001-007, 009-012 |
| 77-87 | Production Reconciliation | PRCID.001-039 |
| 88-103 | Adjustment / Cancellation | ADJID.001-028 |
| 104-114 | Collection of Commission Receivables (Direct Payment) | CMRID.001-015, MKTID.008 |
| 115-120 | Annex: usage requirements, volumes, availability, data retention | NFR |
| 121-123 (scanned) | Approval sheet of the main BRD, signed Jul-Sep 2025 | Operations, Marketing, Risk Management, Comptrollership, Retail / Corporate Marketing |
| 124-132 (scanned) | Annex I-V | Acronyms; Cashiering reports (21); Remittance reports (8); Production reconciliation reports (5); Endorsement slip fields, request types, cancellation reasons |

## 2. Business context

BDOI is a broker. It collects premium from clients **on behalf of insurers**, then remits it to the insurers net of its commission. It also earns commission, service fees, profit share and incentives. Operations "Central" does the following:
- **Cashiering**: receives payments through Bills Payment, OTC, Trade, CLPC loan credits, PDCs and Direct Credit. It issues an **Acknowledgement Receipt (AR)** for premium (trust money) and a BIR **Official Receipt (OR)**, from Head Office only, for BDOI income. It applies payments to booked invoices component by component (DST, VAT, LGT, other charges, basic premium) and keeps unapplied and excess money in a disposition workbench.
- **Remittance**: extracts collected, applied and cleared premium per insurer and remittance type. It builds batches, sends remittance schedules and pushes payment requests to Disbursement. It then uploads the insurer's OR per client, and manages holds and special remittances requested by Marketing.
- **Production Reconciliation**: sends each insurer the production register of booked accounts, uploads the insurer's feedback, matches it within a tolerance of 1.00, and follows unmatched and unbooked accounts to closure.
- **Adjustment / Cancellation**: processes financial, non-financial and internal endorsements on booked accounts, singly or in batches. It recomputes the amounts, posts the accounting entries and sets up excess payments and AR Insurer. It also produces the service invoice (addendum), endorsement slip, validation list and validation slip.
- **Collection of Commission Receivables (Direct Payment)**: some clients pay the insurer directly. For these accounts BDOI validates the account, bills its commission to the insurer, tracks the insurer's feedback within 10 working days, collects the commission (OR) and reverses the premium receivable. The same area covers incentive programmes (No Touch, Top Up, Motor Mania, early remittance) and the tracking of BIR certificates.

All Operations functions start from the **booked invoice** produced by BRD-1 (booking module). Most Operations requirements therefore depend on the booking output: invoice, premium components, DTIP, commission, VAT, WTAX and insurer shares.

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 10 |
| CONFIGURE | Set-up only | 7 |
| CHANGE | Extend or re-purpose existing capability | 44 |
| NEW | New build | 108 |
| OUT | Out of scope per BRD | 0 |
| **Total** | | **169** |

### Rows per section and fit

| Section | Name | Rows | FIT | CONFIGURE | CHANGE | NEW | Effort S/M/L/XL |
|---|---|---|---|---|---|---|---|
| A | General: access, navigation, integration, batch processing (BRQID) | 6 | 2 | 0 | 2 | 2 | 2/3/1/0 |
| B | Cashiering: receipt issuance and management (AR / OR) | 9 | 0 | 0 | 5 | 4 | 3/4/1/1 |
| C | Cashiering: search, audit trail, accounting and series control | 7 | 1 | 0 | 3 | 3 | 5/2/0/0 |
| D | Cashiering: reports and batch printing | 4 | 1 | 0 | 1 | 2 | 2/1/1/0 |
| E | Cashiering: payment matching, application and unapplied disposition | 5 | 0 | 0 | 1 | 4 | 0/3/2/0 |
| F | BIR 2307 reversal flow (Cashiering, Marketing Collection, Disbursement) | 5 | 0 | 0 | 0 | 5 | 0/4/1/0 |
| G | Remittance: extraction and batch processing | 11 | 0 | 1 | 4 | 6 | 8/2/1/0 |
| H | Remittance: insurer remittance schedule and OR update | 4 | 0 | 0 | 2 | 2 | 3/1/0/0 |
| I | Remittance: validation and filtering criteria | 9 | 0 | 0 | 2 | 7 | 7/2/0/0 |
| J | Remittance: search and view | 6 | 0 | 0 | 1 | 5 | 4/2/0/0 |
| K | Remittance: hold and special remittance (incl. Marketing requests) | 10 | 0 | 1 | 3 | 6 | 8/2/0/0 |
| L | Remittance: tracking, notifications, reports and invoice locking | 8 | 0 | 1 | 3 | 4 | 5/3/0/0 |
| M | Production reconciliation: extraction and file management | 11 | 2 | 3 | 4 | 2 | 9/2/0/0 |
| N | Production reconciliation: production register, viewing and filtering | 10 | 0 | 1 | 0 | 9 | 9/1/0/0 |
| O | Production reconciliation: matching, tracking and reports | 18 | 2 | 0 | 1 | 15 | 16/2/0/0 |
| P | Adjustment / cancellation: endorsement transactions | 10 | 0 | 0 | 4 | 6 | 5/3/2/0 |
| Q | Adjustment / cancellation: computation and accounting | 7 | 0 | 0 | 2 | 5 | 1/5/1/0 |
| R | Adjustment / cancellation: documents, reports, traceability | 12 | 2 | 0 | 3 | 7 | 11/1/0/0 |
| S | Collection of commission receivables (Direct Payment) and incentives | 17 | 0 | 0 | 3 | 14 | 9/6/2/0 |

### Rows per ID family

| Family | Area | Rows |
|---|---|---|
| BRQID | General | 6 |
| CSHID | Cashiering | 27 |
| RMTID | Remittance | 40 |
| PRCID | Production reconciliation | 39 |
| ADJID | Adjustment / cancellation | 28 |
| CMRID | Commission receivables (DP) | 15 |
| MKTID | Marketing activities feeding Operations | 13 |
| DBMID | Disbursement activity | 1 |
| **Total** | | **169** |

Addendum updates are recorded on the updated row, as the BRD numbering is unchanged: **RMTID.002** (exclusion instead of editing) and **ADJID.014** (service invoice when income or commission is affected).

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Receivables (insurer model) | Re-use patterns, not the entity | Receipt / allocation / PDC / deposit / bank reconciliation patterns reused; broker AR / OR, series control, reinstatement and component application are new in `cashiering` |
| Accounting engine | Re-use | 20 new Operations event types (plus booking endorsement events); rules configured by Comptrollership (maker-checker) |
| Sub-ledger (open items) | Re-use + read model | Client PR, DTIP, AR Insurer, commission receivable as open items; component and remittance detail in the new `opsledger` invoice ledger |
| Payables | Candidate adapter | Default DisbursementGateway adapter could create payment requests to insurers |
| Tax (BIR) | Extend | Intake of client-issued 2307 (CWT on premium) and insurer certificates on commissions; VAT / WTAX on ORs |
| Bulk | Extend | TXT fixed-width reader, file-hash duplicate block, outcome categories, reprocess failed rows |
| Workflow | Re-use | 10 new workflows (receipt cancel / reinstate, disposition, 2307, remittance batch, hold, special remittance, recon cycle, endorsement, DP billing, BIR certificate) |
| Messaging / docgen | Re-use | Remittance schedules, cover letters, billing files, AR / OR, slips; password protection |
| Reports | Extend | ~45 Operations reports; view vs export permission; generated-report archive |
| Security | Extend | Operations permissions and roles |

### Target modules

| Module | Rows |
|---|---|
| `remittance` | 43 |
| `prodrecon` | 39 |
| `cashiering` | 28 |
| `adjustment` | 28 |
| `commission` | 17 |
| `opsledger` | 8 |
| `platform (security / bulk / report)` | 5 |
| `frontend (features/operations) + each module` | 1 |

## 4. Operations flow (for concurrence)

| Step | Owner | What happens | BRD |
|---|---|---|---|
| Booking (BRD-1) | Processing | Invoice created: PR by component (client), DTIP (insurer), commission and VAT; DP flag; insurer shares | BRNB.027/114 |
| Payment intake | Cashier / system | Payment files, OTC, PDC maturity, check pick-up; AR issued (branch series) | CSHID.001/006/008/009 |
| Matching and application | System | Booked: apply by component hierarchy (98% for CWT). Pre-booked: wait and re-run. Otherwise unapplied / excess | CSHID.020/022 |
| Unapplied disposition | Cashier / Marketing / TL | Apply to other invoice, DST application, refund, reclass, transfer; approvals; <= PHP 10 to AP overages | CSHID.016/024/025 |
| Remittance extraction | System (scheduled) / Processor | Applied and posted, checks >= 3 banking days and cleared, not on hold, no pending negative adjustment, not written off, paid AR <= DTIP | RMTID.001-023 |
| Process remittance | Remittance processor / TL | Review, exclude (addendum), submit, approve; remittance schedule and payment request; push to Disbursement; commission OR per settlement batch | RMTID.002/009-011/029, CSHID.007 |
| Insurer OR update | Remittance processor | Upload insurer-populated schedule; OR vs paid PR exception report | RMTID.012/013/016 |
| Production reconciliation | Recon handler | Extract register per insurer, send, upload feedback, match (tolerance 1.00), buckets, dispositions, reports | PRCID.* |
| Adjustment / cancellation | Marketing request, Adjustment processor | Endorsement request, recompute, approve, batch post, service invoice, slip, validation list; excess -> unapplied; remitted -> AR Insurer | ADJID.* |
| Direct payment commission | Commission handler | DP lists -> validate -> sanitise -> bill insurer -> feedback (10 working days) -> collect (OR) -> PR reversal | CMRID.*, MKTID.011/012 |
| BIR 2307 | Marketing Collection, Cashier, Disbursement | Tagging -> validation vs CWT copies -> 2307 report -> Disbursement -> release to insurer | CSHID.026/027, MKTID.010/013, DBMID.001 |

## 5. Requirements and fit/gap

### A. General: access, navigation, integration, batch processing (BRQID)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| BRQID.001<br><sub>Main BRD p.16</sub> | Operations | Access and log in to the system | Correct credentials give access; otherwise access denied | **FIT** | JWT login with roles; lockout; session timeout | Re-use; Operations roles seeded (see OPERATIONS_DESIGN §6). BDO SSO / AD stays parked (BRD-1 Q42) | `platform (security / bulk / report)` | S | OQ48 |
| BRQID.002<br><sub>Main BRD p.16</sub> | System | Restrict access to authorised users only | Only authorised users log in; invalid credentials denied; unauthorised attempts refused | **FIT** | Role -> permission model, @PreAuthorize on every endpoint, audit of logins | Add Operations permissions and roles; nothing else | `platform (security / bulk / report)` | S | OQ48 |
| BRQID.003<br><sub>Main BRD p.17</sub> | Operations | After login present Operations functionality prominently on the dashboard; role-based sections (Cashiering, Remittance, Prod Recon, Adjustment, Commission Receivables) each with full functions, validations, reports and links to integrated applications; switch between sections | Operations visible immediately; only role-relevant sections; user can customise / filter sections; switching works; section load < 5 s; no blank pages; every section has reports and links | **CHANGE** | Role-filtered sidebar generated from FeatureModule; My Work queues; executive dashboard widgets | Operations home page: one tile per section (counts from each module's /counts endpoint, SLA badges), pinned-section preference per user (local preference), external-application link list maintained as LOV OPS_EXTERNAL_LINK | `frontend (features/operations) + each module` | M | OQ48 |
| BRQID.004<br><sub>Main BRD p.18</sub> | System | Integrate with other systems / modules to fetch or flow in data (Collection, Accounting, Disbursement, Marketing, Claims) | Integration endpoints established and tested for each external system; failures from endpoint/network handled | **NEW** | No external interfaces for broking; GL/accounting is internal (accounting engine); payables module exists | Inbound/outbound port per partner system (CollectionFeed, DisbursementGateway, MarketingFeed, ClaimsFeed, AccountingExport) with a default adapter that is manual upload / internal module; real transports parked until BDOI gives specs | `opsledger` | L | OQ01,OQ02,OQ46 |
| BRQID.005<br><sub>Main BRD p.18</sub> | System | Fetch required data from other systems automatically (real-time or scheduled), validated and mapped, with fetch logs and alerts on failure | All required fields fetched; validated and mapped; no manual intervention (on demand or scheduled job); log per fetch with timestamp and status; alert on failed / mismatched fetch; no duplicates | **NEW** | ManagedJob scheduler with run history; alert framework; bulk validation | Flow-in framework in opsledger: ops_flow_in_run (source, feed, trigger, counts, status, errors) and ops_flow_in_record (idempotency key, payload hash); each feed is a ManagedJob; OPS_FLOW_IN_FAILED alert; the transport per feed is a port (file drop / upload now) | `opsledger` | M | OQ01 |
| BRQID.006<br><sub>Main BRD p.19-20</sub> | System | Batch jobs continue when records fail; failed records logged for review and reprocessing; end-of-run report with successful / unsuccessful uploads, applied, unapplied and unsuccessful records | Job does not stop on a failed record; valid records processed; failures with error messages stored in a separate table; post-run report with counts and lists per category; failed records can be reviewed and reprocessed | **CHANGE** | bulk framework: per-row transaction, partial commit, error report XLSX, bulk_job/bulk_row audit | Extend bulk: handler-defined outcome categories (APPLIED / UNAPPLIED / PREBOOKED ...), 'reprocess failed rows' action, run report PDF/XLSX with the five sections; same pattern reused by scheduled jobs (auto-matching, extraction) | `platform (security / bulk / report)` | M |  |

### B. Cashiering: receipt issuance and management (AR / OR)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| CSHID.001<br><sub>Main BRD p.23-24</sub> | Operations (Cashier) | Create, cancel and reinstate Acknowledgement Receipts (AR) for premium payments (Bills Payment, OTC, Trade, CLPC, PDC) and non-premium payments (insurance payments: refund, other expenses, AR Insurance) | Required fields; AR number auto-generated per branch from a maintained series; series not depleted; AR in received currency; cancel / reinstate reason mandatory; full or partial reinstatement; accounting on cancel / reinstate; JEs for uploaded applied and unapplied; reinstatement transaction number; audit. Negative: cancel without reason, reinstate without required fields, duplicate transaction no. | **CHANGE** | receivables Receipt (OR) with maker-checker, cancel / bounce reversal with negative events, OR-<branch>-<yyyy> numbering; no AR type, no reinstatement, no BIR series | cashiering: CollectionReceipt with receiptKind AR/OR, receiptClass (PREMIUM / NON_PREMIUM / COMMISSION / SERVICE_FEE ...), series-controlled numbering, cancel and reinstate (full / partial) as separate transactions with own transaction number, reversal events; payment application in cashiering (see E) | `cashiering` | L | OQ05,OQ06 |
| CSHID.002<br><sub>Main BRD p.24-25</sub> | Operations (Cashier) | Create, cancel and reinstate Official Receipts (OR) for Service Fee Income (risk management, consultancy), Insurance Profit Share, Commissions (with VAT / W-tax), Incentives, Others (discount) | Same controls as AR: required fields, series validated and not depleted, currency, reasons for cancel / reinstate, accounting on cancel / reinstate, reinstatement transaction number, audit | **CHANGE** | receivables Receipt with MISC_RECEIPT to a chosen income account; VAT / EWT not split on receipts | OR types as LOV OR_TYPE driving the accounting event (OPS_OR_ISSUE with components GROSS, VAT, WTAX, NET); OR only by head office (CSHID.006); commission ORs from remittance settlement batches and DP collections | `cashiering` | M | OQ05,OQ06,OQ07 |
| CSHID.003<br><sub>Main BRD p.25-26</sub> | System | Choose the reason for AR or OR cancellation from maintained lists (premium, commission, PDC retrieval / replacement, bounced check, issuance error, printing error, check not picked up, double issuance, others) | Reason mandatory; invalid / deprecated reasons refused; reason saved on the record | **CHANGE** | Free-text reversal reason on receipts; LOV service with effectivity and maker-checker | LOV RECEIPT_CANCEL_REASON with parent groups (PREMIUM, COMMISSION, PDC, GENERAL); 'Others' requires text; bounced check reinstatement skips the reinstatement form (BRD note) | `cashiering` | S | OQ06 |
| CSHID.004<br><sub>Main BRD p.26-27</sub> | System | Choose the reinstatement reason: premium payment reinstatement (due to cancellation, mis-application, W-tax adjustment, others) and direct payment reinstatement (double reversal, wrong OR details, W-tax adjustment, due to cancellation, others) | Reason mandatory and saved; invalid / deprecated reasons refused; journal entry created; failure to save blocks reinstatement | **NEW** | No reinstatement concept | LOV REINSTATEMENT_REASON (groups PREMIUM / DIRECT_PAYMENT); reinstatement posts OPS_RECEIPT_REINSTATE (premium) or OPS_DP_REINSTATE (direct payment) | `cashiering` | S | OQ06 |
| CSHID.005<br><sub>Main BRD p.27-28</sub> | System | Encode required information for reinstatement: premium (invoice no., AR no., assured / payor, amount reinstated, AO, Unit Head, Team Leader); direct payment (invoice no., OR no., assured / payor, amount reinstated) | Reason and fields mandatory; reinstated transaction reflects original details; missing fields block reinstatement | **NEW** | - | Reinstatement form per group with defaults from the original receipt; AO / UH / TL picked from the sales organisation (catalog) | `cashiering` | S | OQ06 |
| CSHID.006<br><sub>Main BRD p.28-29</sub> | System | Auto-assign AR / OR numbers per transaction by branch; OR issued exclusively by Head Office | AR numbers for branch transactions, OR numbers only for HO; sequential / predefined format generated on posting; OR from non-HO branch refused; no duplicates; no transaction without a number | **CHANGE** | DocumentNumberService gapless series per prefix; Branch.headOffice flag | ReceiptSeries master (branch, kind AR/OR, BIR ATP no., from / to, next, warning threshold, active) with maker-checker; number allocated at posting under row lock; OR allowed only when branch.headOffice | `cashiering` | M | OQ05 |
| CSHID.007<br><sub>Main BRD p.29</sub> | System | Issue one OR per commission check / credited amount, or one OR for several payments of the same insurer / payee and certificate; sources: commission payment details uploaded from Collection, Remittance Schedule | Correct payee and amount; one OR per remittance batch number; consolidation only for same insurer / payee and same certificate; mismatches rejected; missing details flagged | **NEW** | One receipt per payer with manual allocation | Commission OR builder: groups commission lines by insurer + certificate; source = remittance settlement batch (remittance calls cashiering OrService) or COMMISSION_PAYMENT upload; validation of consolidation keys | `cashiering` | M | OQ49 |
| CSHID.008<br><sub>Main BRD p.29-32</sub> | System | For AR, upload Bills Payment (TXT FS01 from IT-DCO), Trade (TXT from CIB), CLPC (Excel), matured PDC (Excel) and Direct Credit (TXT FS01/04) files for batch processing; PDC warehouse with system number, auto-transaction at maturity and AR on application | Agreed format and file type; files encrypted / not editable; auto-matching on the defined run frequency; AR issued for all uploaded accounts; field lists per file (bills: incremental #, invoice #, assured, amount, late deposit Y/N, phone, branch code, payment date / time; trade; CLPC; direct credit); report of applied / unapplied; unmatched to unapplied premium bucket; PDC list stored, viewable, traceable number | **CHANGE** | bulk framework (XLSX / CSV / ODS); receivables PDC register (ON_HAND -> DUE -> DEPOSITED raises an OR) | Five bulk handlers PAY_BILLS, PAY_TRADE, PAY_CLPC, PAY_PDC, PAY_DIRECT_CREDIT; add fixed-width / delimited TXT reader and file-hash duplicate block to bulk; uploaded file stored read-only with SHA-256; payments land in a PaymentBatch and go through the matching engine (E); PDC warehouse (cashiering PdcItem, number PDCW-<yyyy>) with daily maturity job creating the payment and AR on application | `cashiering` | XL | OQ03,OQ04,OQ12 |
| CSHID.009<br><sub>Main BRD p.32-33</sub> | System | Accept checks tagged 'for check pick-up' from Collection: queue in Cashiering, filter by pick-up date, assign unique AR number at filtering / printing, show only accounts due for printing | Only 'for check pick' status queued; filter by pick-up date; unique AR assigned and logged; accounts not due excluded; integration failures handled | **NEW** | - | CheckPickupRequest inbound (CollectionFeed port; manual / upload adapter now); queue screen with date filter; 'print ARs' allocates AR numbers in one transaction; report 'Check Pick-Up Request' | `cashiering` | M | OQ01,OQ13 |

### C. Cashiering: search, audit trail, accounting and series control

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| CSHID.010<br><sub>Main BRD p.33-34</sub> | Operations | Search by AR no., OR no., client no., invoice no., payor, assured, amount, date of issuance, policy no., insurer name | All parameters available, single or combined; exact and partial (names) match; readable results; large volumes without timeouts; no impact on other functions; search logs kept | **CHANGE** | ReceiptSearch (receipt no., party, dates, status) | Receipt / payment search with the ten criteria, trigram index on names, paged; search log (user, criteria, time) in ops_search_log | `cashiering` | S | OQ47 |
| CSHID.011<br><sub>Main BRD p.34-35</sub> | System | Audit trail records AR number and username for every action | AR no. + username + timestamp + action + module; immutable; searchable by AR no. or username; near real time | **FIT** | Immutable audit trail (DB-enforced) keyed by entity and user; audit report with filters | Record audits with entity key = AR/OR number; add AR number filter on the audit viewer | `cashiering` | S |  |
| CSHID.012<br><sub>Main BRD p.35</sub> | System | Post accounting entries when a cancellation is posted | JEs created and posted automatically; peso conversion; exchange rate 2 decimals from the Comptrollership exchange-rate table; correct FX | **CHANGE** | Receipt cancellation posts the receipt events with negative amounts; SPOT rate numeric(19,8) | Cancellation re-posts the original receipt and application events with negative amounts (source ref ...:CANCEL:<n>); rate type BOOK (Comptrollership table) rounded to 2 decimals passed on the BusinessEvent for all Operations postings | `cashiering` | S | OQ07,OQ08 |
| CSHID.013<br><sub>Main BRD p.35-36</sub> | System | Post accounting entries when a reinstatement is posted | As CSHID.012 | **NEW** | - | OPS_RECEIPT_REINSTATE event; re-application of the reinstated amount goes through the application engine | `cashiering` | S | OQ07,OQ08 |
| CSHID.014<br><sub>Main BRD p.36</sub> | System | Post accounting entries for AR / OR issuance | As CSHID.012 | **CHANGE** | PREMIUM_RECEIPT / PREMIUM_DEPOSIT / MISC_RECEIPT events | Broker events: OPS_AR_RECEIPT (cash in to unapplied / trust), OPS_PAYMENT_APPLY (by PR component, with commission realization), OPS_OR_ISSUE (income, VAT, WTAX by OR type); unapplied stays in the unapplied collections account; rules configured by Comptrollership | `cashiering` | M | OQ07,OQ08 |
| CSHID.015<br><sub>Main BRD p.37</sub> | System | Validate that the AR / OR series is not depleted before processing | Transaction blocked with an error when the series is depleted; availability checked first | **NEW** | Series never run out (unbounded counters) | ReceiptSeries range check before allocation; RECEIPT_SERIES_LOW alert at threshold; RECEIPT_SERIES_DEPLETED business error | `cashiering` | S | OQ05 |
| CSHID.016<br><sub>Main BRD p.37-38</sub> | System | Automatically reverse premium receivables with minimal balances (PHP 10.00 and below) unless the balance equals the 2% CWT, the DST charged or the entire premium; no duplicate reversal | Refers to a maintained minimal-balance table; identifies balances <= 10.00; compares with the PR2307 balance; reverses only when different; audit; never twice | **NEW** | Open-item sub-ledger with balances; ManagedJob framework | MINIMAL_BALANCE_SWEEP job over opsledger invoice balances; table ops_minimal_balance_rule (kind PREMIUM / COMMISSION / EXCESS, max amount, exclusion rules, GL target); event OPS_MINIMAL_BALANCE_REVERSAL idempotent on invoice + component; excess (credit) minimal balances go to AP overages (summary 5.f) | `cashiering` | M | OQ07,OQ11 |

### D. Cashiering: reports and batch printing

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| CSHID.017<br><sub>Main BRD p.38</sub> | System | Authorised users generate, download or print reports | Reporting interface; parameters (date range, type); dynamic generation; PDF / Excel download; direct print; permission-restricted | **FIT** | Report framework: parameter forms, PDF / XLSX / CSV export, role-based catalogue | Re-use; add OPS_REPORT_EXPORT permission separate from view (see CSHID.018) | `platform (security / bulk / report)` | S |  |
| CSHID.018<br><sub>Main BRD p.38</sub> | System | Selected users can view generated reports (list, content, metadata) but cannot download / print unless permitted | View-only rights enforced; metadata (name, date, creator) shown | **CHANGE** | Report view and export share one permission; no generated-report archive | Generated-report archive (report_run: code, parameters, creator, time, file) and split permissions OPS_REPORT_VIEW / OPS_REPORT_EXPORT in the report framework | `platform (security / bulk / report)` | S |  |
| CSHID.019<br><sub>Main BRD p.39-40</sub> | System | Batch printing of AR / OR from a selection or filter (e.g. AR per BDOI location, OR per insurer); save copies; confirmation, reprint / download | Multi-select or filter; list shown before printing; standard layout; print or one downloadable file; 50 documents < 10 s; log user / time / count; failures notified with retry or skip; authorised only | **NEW** | Single receipt PDF via DocumentComposer | Batch print job: merged PDF of AR / OR templates (docgen), copy stored per receipt (attachment), print log ops_print_log; permission CASH_PRINT | `cashiering` | M | OQ05 |
| CSHID.023<br><sub>Main BRD p.42,125-127</sub> | System | Generate the Cashiering reports listed in the Annex | Expected format and data; save / download / print by authorised users; authorised view | **NEW** | Report framework; FIN-AR ageing and PDC reports (insurer model) | 21 Cashiering reports (see CASHIERING_REPORTS) as ReportDefinitions in cashiering.report; fields for reports 9-18, 20-21 to be confirmed | `cashiering` | L | OQ42,OQ43 |

### E. Cashiering: payment matching, application and unapplied disposition

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| CSHID.020<br><sub>Main BRD p.40-41</sub> | System | Match at payment acceptance (except issued PDC): booked accounts with outstanding PR (apply up to 100% of the balance, 98% for 2% CWT accounts; excess unapplied); otherwise match pre-booked accounts and wait for booking (re-run manual or scheduled; processed items leave the pre-booked table); no match -> unapplied; categorise excess payments and payments against cancelled bookings / slips | Categories booked-matched, pre-booked, no match, excess, matched vs cancelled booking; scheduled and manual re-run | **CHANGE** | AutoMatcher for bank lines; AllocationPlanner FIFO / manual on open items; on-account balance | PaymentMatchingEngine: keys ARN / invoice no. / PN no.; buckets APPLIED, PREBOOKED, UNAPPLIED_NO_MATCH, EXCESS, CANCELLED_REFERENCE; CWT flag on client / invoice limits application to 98%; PREBOOKED_REMATCH ManagedJob + manual trigger; booking event from opsledger triggers immediate re-match | `cashiering` | L | OQ09,OQ10,OQ12 |
| CSHID.021<br><sub>Main BRD p.41</sub> | System | Flow in payments from insurers to AR Insurance, segregated from premium payments | Premium vs non-premium insurer payments distinguished; non-premium posted to AR Insurance the same business day; no mis-routing | **NEW** | Receipts from reinsurers post RI_SETTLEMENT_RECEIPT (insurer model) | Payer = insurer party + receipt class NON_PREMIUM routes to the ARI open items (AR Insurer set up by adjustment / cancellation); event OPS_AR_INSURANCE_RECEIPT | `cashiering` | M | OQ14 |
| CSHID.022<br><sub>Main BRD p.42</sub> | System | Apply payments by hierarchy: per transaction PR-DST, PR-Premium Tax (VAT), PR-LGT, PR-Other Charges, PR-Basic Premium; mode of payment hierarchy Check, Cash | Each category applied in order using the highest-priority mode available | **NEW** | Allocation is per open item, not per component | Component-level allocation in the matching engine over opsledger invoice components (hierarchy table ops_application_hierarchy, seeded as BRD); requires booking to publish the PR by component | `cashiering` | M | OQ09 |
| CSHID.024<br><sub>Main BRD p.42-43</sub> | System | View and manage dispositions of unapplied payments (apply to other invoice, DST payment application, refund, reclass, transfer to other marketing unit, others); approvals and reversals routed and logged; disposition types configurable | Assign disposition to any unapplied payment; payments move tabs by status; approvals routed; actions logged; update before final processing; only authorised users approve / reverse / maintain types | **NEW** | Unapplied (on account) balance can be applied later; no disposition workflow | UnappliedPayment + Disposition entity; LOV DISPOSITION_TYPE with attributes (requires approval, action: APPLY / REFUND / RECLASS / TRANSFER); workflow OPS_DISPOSITION; refund disposition flows to Collection / Disbursement through the ports | `cashiering` | L | OQ15 |
| CSHID.025<br><sub>Main BRD p.43-44</sub> | System | Disposition tabs: Unapplied (no disposition), Monitoring (disposition assigned), For Approval (team approval), For Reversal (marked for reversal, with reason, submit and track) | View details, assign type, track status (pending / in process / completed) and history; For Approval triggered by status | **NEW** | - | Tabbed Unapplied Payments workbench driven by OPS_DISPOSITION stages | `cashiering` | M | OQ15 |

### F. BIR 2307 reversal flow (Cashiering, Marketing Collection, Disbursement)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| CSHID.026<br><sub>Main BRD p.44</sub> | System | Receive BIR 2307 data tagged by Marketing Collection; cash payments: issue AR, reverse premium, process remittance; certificate payments: receive certificate details and reflect status; sort and forward to Disbursement | Tagged data received; cash vs certificate paths; transactions sorted and forwarded to Disbursement | **NEW** | tax module issues 2307 certificates (as payor); no intake of client-issued 2307 | Cwt2307Tag intake (MarketingFeed port / screen); cash path issues AR and applies to the PR2307 component; certificate path records CertificateReceipt with status; hand-off to Disbursement through DisbursementGateway | `cashiering` | L | OQ01,OQ10,OQ16 |
| CSHID.027<br><sub>Main BRD p.44-45</sub> | System | Process BIR 2307 reversals using Marketing reference numbers: validate against CWT copies, generate the BIR 2307 transaction report, post and route directly to Disbursement (bypassing Remittance), zero out PR | Reference input shows reversal details; batch validated vs CWT copies; printable report; posted and routed; certificates sorted per insurer; PR zeroed; entries Dr PR2307 / Cr PR or Dr DTIP / Cr PR2307; attachments. Negative: invalid reference, missing CWT copy, data mismatch, report / routing failure, wrong entries | **NEW** | - | Cwt2307ReversalBatch (reference, lines, validation, report PDF, attachments); events OPS_CWT_RECLASS (Dr PR2307 / Cr PR) and OPS_CWT_DTIP_OFFSET (Dr DTIP / Cr PR2307); workflow OPS_CWT_2307 | `cashiering` | M | OQ07,OQ16 |
| MKTID.013<br><sub>Main BRD p.46</sub> | Marketing | Tag reversal transactions for BIR 2307 and submit certificates received from clients; flows to Cashiering | Reference numbers generated and stored at tagging; accessible to Cashiering by reference; data complete for validation | **NEW** | - | Marketing Collection screen 'BIR 2307 tagging' (reference CWT-<yyyy>) until the Collection system feeds it (MarketingFeed port) | `cashiering` | M | OQ45,OQ16 |
| MKTID.010<br><sub>Main BRD p.74-75</sub> | Marketing | Marketing ensures 2307 certificates are tagged, 2307 selected with correct withholding % at AR/OR creation, CWT details complete, reinstatement requests submitted with reason and payment details, and confirms if already remitted (reversal to Unapplied vs Adjustment); if remitted, written insurer confirmation of refund | Integrated Marketing Collection 2307 reversal and Cashiering workflow; certificates tracked; all actions logged with user, time and reason | **NEW** | - | Same tagging screen plus reinstatement request (feeds CSHID.004/005) and a 'remitted?' check read from opsledger; routes to Unapplied (cashiering) or to an adjustment request; insurer confirmation as mandatory attachment | `cashiering` | M | OQ45,OQ16 |
| DBMID.001<br><sub>Main BRD p.46</sub> | Disbursement | Receive and process the BIR 2307 report and sorted certificates from Cashiering; complete posting so PR is zeroed with Dr PR2307 / Cr PR or Dr DTIP / Cr PR2307; reverse DTIP; release to insurers | Transactions and per-insurer reports received; certificates sorted and complete with documents; correct entries; DTIP reversed; released to insurers | **NEW** | payables (supplier invoices, vouchers) could act as Disbursement | Outbound DisbursementGateway.send2307Report(...); default adapter = Disbursement work queue in BrokerVerse (acknowledge, release to insurer) until the Disbursement system / BRD is known | `cashiering` | M | OQ02 |

### G. Remittance: extraction and batch processing

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| RMTID.001<br><sub>Main BRD p.49</sub> | System | Extract per insurer and remittance type (With Incentives, Normal-Dollar, Normal-Peso); off-peak; batch run TBD | Select insurer and type; only matching accounts; type tags; saved to shared drive with one folder per type; naming convention; exclude pending financial endorsements (cancellation, decrease TSI / commission); no performance impact; result notification | **NEW** | - | RemittanceExtraction service over opsledger invoice balances; RemittanceType LOV; extract file XLSX via docgen stored in the extract repository (FileDropPort for the shared drive, parked); notification to REMIT_PROCESS holders | `remittance` | L | OQ17 |
| RMTID.002<br><sub>Main BRD p.50 + UPDATED by Addendum 1 p.4-5</sub> | System | ADDENDUM: view the extracted file online and mark records for exclusion only (no editing of financial data); exclusions tracked and reversible before submission; only non-excluded rows pushed to Disbursement with a full exclusion audit | Grid view with exclusion controls and summary; financial fields read-only; preview of resulting dataset; restore before final submission; authorised users only, all logged. Negative: editing financial fields, excluding rows that remove mandatory fields, unauthorised exclusion. (Main BRD allowed online editing and push with or without edit - superseded) | **NEW** | - | Extraction line grid with exclude / restore (reason LOV REMIT_EXCLUSION_REASON), ops audit, submission preview; no edit endpoint for amounts; permission REMIT_EXCLUDE | `remittance` | M | OQ48 |
| RMTID.003<br><sub>Main BRD p.50-51</sub> | System | Automatic extraction on predefined schedules and criteria; accounts not meeting criteria stay tagged 'not yet for remittance' | Scheduler configurable by admin; runs logged; auto-tags Extracted / Unextracted not yet due / Unextracted due / Returned | **CHANGE** | ManagedJob with configurable cron and run history | REMITTANCE_EXTRACTION ManagedJob per insurer / type (cron in application.yml + sys_parameter); extraction tag on the invoice remittance state | `remittance` | S | OQ17 |
| RMTID.004<br><sub>Main BRD p.51-52</sub> | Operations | Manual extract for a specific invoice number per insurer | Invoice input; data matches invoice and insurer; confirmation; invalid / unposted invoices refused | **NEW** | - | Manual extraction endpoint and dialog with the same eligibility checks | `remittance` | S |  |
| RMTID.005<br><sub>Main BRD p.52</sub> | System | Search account and payment details per invoice or batch for remittance processing; trigger extraction at end of day if payment application is searched | Search logs trigger extraction; extracted accounts match; EOD timestamp; no multiple triggers per search | **NEW** | - | Implement as: invoices looked up for remittance during the day are queued for the EOD extraction run (idempotent queue); semantics to confirm | `remittance` | S | OQ18 |
| RMTID.006<br><sub>Main BRD p.52</sub> | System | Extract only from applied and posted payments (cash or check, all channels: credit to account, ADA, OTC, bills payment...) | Unposted / unapplied excluded; payment status shown and logged at extraction | **NEW** | - | Eligibility rule reads opsledger applied-and-posted amounts per invoice (paid AR) | `remittance` | S |  |
| RMTID.007<br><sub>Main BRD p.52-53</sub> | System | Group extract by remittance type, processor, insurer and batch number | Grouping in UI; correct categorisation; unique batch numbers; download and view by authorised users | **NEW** | - | RemittanceBatch per insurer + type; processor assignment from insurer-processor mapping | `remittance` | S | OQ17 |
| RMTID.008<br><sub>Main BRD p.53</sub> | System | Unique reference / batch number for each remittance extract | System-generated, non-repeating, traceable to extraction log, searchable | **CONFIGURE** | DocumentNumberService gapless series | Series RMB-<insurer>-<yyyy> | `remittance` | S |  |
| RMTID.009<br><sub>Main BRD p.53-54</sub> | System | Transfer extracted accounts to Process Remittance with status 'Review in process'; re-assign to another processor | Transfer logged; statuses updated; no loss or duplication | **CHANGE** | workflow engine: cases, assign / claim, queues | Batch starts OPS_REMITTANCE case at REVIEW_IN_PROCESS; re-assign via WorkAssignmentService | `remittance` | S |  |
| RMTID.010<br><sub>Main BRD p.54</sub> | Operations | Submit accounts for remittance processing | Submission triggers workflow; defined status; confirmation; no double submission | **CHANGE** | workflow transitions | Transition submit (REVIEW_IN_PROCESS -> FOR_APPROVAL) with validation of RMTID.019 | `remittance` | S | OQ21 |
| RMTID.011<br><sub>Main BRD p.54-55</sub> | Operations | Print and save the remittance schedule and payment request | Print / save after submission; batch no., insurer, type and payment details; consistent format | **CHANGE** | docgen PDF / XLSX composer | Remittance Schedule (Normal / Special / With Incentives layouts per Annex III) and Payment Request documents, versioned templates, stored on the batch | `remittance` | M | OQ42 |

### H. Remittance: insurer remittance schedule and OR update

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| RMTID.012<br><sub>Main BRD p.55-56</sub> | System | Receive the populated remittance schedule from the insurer with OR details per client (OR date and number); validate integrity and authenticity | Secure predefined channel (e.g. upload); accepted format; complete OR data. Negative: missing OR no. / date, same OR no. twice for a client, unreadable file | **NEW** | - | Bulk handler REMIT_INSURER_OR (upload now; insurer SFTP / e-mail parked); checks batch no., insurer, invoice membership | `remittance` | M | OQ22 |
| RMTID.013<br><sub>Main BRD p.56</sub> | System | Upload the received remittance schedule; update client records with insurer OR date and number; confirm the update | Secure upload; records updated after validation; unsupported / corrupt / incomplete files refused | **NEW** | - | Commit updates InsurerOr on the invoice (opsledger) and the batch line | `remittance` | S | OQ22 |
| RMTID.016<br><sub>Main BRD p.57-58</sub> | System | After upload, generate an Exception Report comparing insurer OR amount with paid PR per transaction | Auto-generated after each upload; reference, OR amount, paid PR, status, failure reason; summary; Excel / PDF; accessible from upload history | **CHANGE** | bulk error report (XLSX) | Exception report as bulk outcome report + ReportDefinition REM-OR-EXCEPTION | `remittance` | S | OQ22 |
| MKTID.001<br><sub>Main BRD p.70-71</sub> | Marketing | Initiate a request to send a remittance schedule to the insurer with client details (client ID, amount, date); confirm sending | Details captured and validated; schedule sent; confirmation; no duplicate send | **CHANGE** | messaging outbox with send log and encryption | 'Send schedule' request on the batch (Marketing may request, Remittance sends) via MessageService with protected XLSX; duplicate-send guard | `remittance` | S | OQ22,OQ45 |

### I. Remittance: validation and filtering criteria

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| RMTID.014<br><sub>Main BRD p.56-57</sub> | System | Paid AR not more than outstanding DTIP per remittance; if collected > DTIP remit only the DTIP balance | Rule enforced at extraction; Paid AR > DTIP flagged and excluded (AC) - conflicts with 'remit DTIP balance' (expected result); negative DTIP not extracted | **NEW** | - | Eligibility rule with configurable behaviour CAP \| EXCLUDE until BDOI decides; exclusion reason recorded | `remittance` | S | OQ19 |
| RMTID.015<br><sub>Main BRD p.57</sub> | System | List of Paid AR higher than the DTIP balance | Invoice no., insurer, paid AR, DTIP; per run or real time; excluded accounts marked | **NEW** | Report framework | Report REM-PAIDAR-OVER-DTIP | `remittance` | S | OQ19 |
| RMTID.017<br><sub>Main BRD p.58-59</sub> | System | Checks at least 3 banking days old and cleared before inclusion (holding period) | Younger or uncleared checks excluded; applied at extraction; cleared status visible | **CHANGE** | Holiday calendar (organization); deposit slips; bank reconciliation matches | Holding-period rule: banking-day calculator on organization holidays, parameter REMIT_CHECK_HOLD_DAYS = 3; cleared = deposit matched in bank reconciliation or cleared flag from bank file | `remittance` | M | OQ20 |
| RMTID.018<br><sub>Main BRD p.59</sub> | System | Validate check holding period and clearance together | Both checked; only eligible checks; validation logs | **CHANGE** | As RMTID.017 | Same rule; per-line validation log in the extraction run | `remittance` | S | OQ20 |
| RMTID.019<br><sub>Main BRD p.59-60</sub> | System | Validate remittance status on submission: Approved, Review in Process, Requested for Hold, Unapplied Payment, With Outstanding Balance, Unprocessed, Fully Remitted, Partially Remitted | Only valid statuses submitted; errors for invalid; status updated after submission | **NEW** | - | RemittanceStatus derived on each invoice (opsledger) + workflow stage of the batch; submission guard | `remittance` | S | OQ21 |
| RMTID.020<br><sub>Main BRD p.60-61</sub> | System | Exclude accounts on hold or with pending negative adjustment requests | Both conditions checked; excluded accounts reported; only eligible extracted | **NEW** | - | Invoice flags ON_HOLD and PENDING_NEG_ADJ in opsledger (set by remittance hold and adjustment); exclusion report | `remittance` | S |  |
| RMTID.021<br><sub>Main BRD p.61</sub> | System | Process accounts when the holding date expires; hold information flows in from Marketing | Processing allowed past hold date; logged; reports and dashboards reflect it | **NEW** | - | HOLD_EXPIRY job releases expired holds (flag cleared, invoice eligible for next extraction) | `remittance` | S | OQ24 |
| RMTID.022<br><sub>Main BRD p.61-62</sub> | System | Exclude written-off accounts | Written-off excluded from all outputs, still visible and marked in audit / detail views | **NEW** | - | WRITTEN_OFF flag on the invoice (set by adjustment write-off / minimal balance) | `remittance` | S | OQ26 |
| RMTID.023<br><sub>Main BRD p.62</sub> | System | Automate the additional incentive for early remittance to the insurer within 30 days from inception | Remittance date checked; incentive applied only within 30 days. Negative: incentive despite late remittance or missing insurer data | **NEW** | - | EarlyRemittanceIncentive rule table (insurer, product / segment, rate, window days, basis date); computed on 'With Incentives' batches; event OPS_REMIT_INCENTIVE | `remittance` | M | OQ23 |

### J. Remittance: search and view

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| RMTID.024<br><sub>Main BRD p.62-63</sub> | System | View extracted accounts | Searchable, filterable list with insurer, type, batch; real time | **NEW** | - | Extraction workbench list | `remittance` | S |  |
| RMTID.025<br><sub>Main BRD p.63</sub> | System | Search accounts and payment details by invoice no., remittance batch no., endorsement reference no., policy no., assured name | Fields functional; exact and partial; not case- / format-sensitive | **NEW** | - | Remittance search (case-insensitive, trigram on names) | `remittance` | S |  |
| RMTID.026<br><sub>Main BRD p.63</sub> | Operations | View account / invoice details | Payment status, remittance type, insurer, history; from search results; authorised data only | **NEW** | - | Invoice 360 panel from opsledger (components, payments, remittances, adjustments, holds) | `opsledger` | M |  |
| RMTID.027<br><sub>Main BRD p.64</sub> | Operations | View accounts submitted from Process Remittance | Listed with status and batch; real time; filters and sorting | **CHANGE** | workflow queues | Queue view on OPS_REMITTANCE stages | `remittance` | S |  |
| RMTID.028<br><sub>Main BRD p.64</sub> | Operations | Check payment status before extraction or processing | Only valid statuses (posted, cleared); invalid / pending excluded; status visible | **NEW** | - | Payment status read from cashiering through opsledger; shown on lines | `remittance` | S | OQ20 |
| RMTID.038<br><sub>Main BRD p.68-69</sub> | System | Include, record and update remittance details at invoice level with visible payment history (dates, amounts applied, AR numbers) | Add / edit / view application details at invoice level; complete history; real-time and traceable; audit and reconcile | **NEW** | Open items with matches (party level) | opsledger invoice movement history (booking, applications, reversals, remittances, adjustments) - one read model for all Operations screens | `opsledger` | M |  |

### K. Remittance: hold and special remittance (incl. Marketing requests)

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| RMTID.029<br><sub>Main BRD p.64-65</sub> | Operations | Submit, hold and return invoices or batch numbers | Status updates; hold tags; return with reason; logged; returned items leave the queue | **CHANGE** | workflow transitions with reason | Batch- and line-level actions submit / hold / return (reason LOV REMIT_RETURN_REASON) | `remittance` | S |  |
| RMTID.030<br><sub>Main BRD p.65</sub> | Operation | View special remittance requests flowing in from Marketing Collection | Requests visible with status and details | **NEW** | - | SpecialRemittanceRequest list (created in BrokerVerse or via CollectionFeed) | `remittance` | S | OQ25 |
| RMTID.031<br><sub>Main BRD p.65-66</sub> | System | Exclude active hold accounts during extraction | Hold checked automatically; exclusions logged | **NEW** | - | Same rule as RMTID.020 | `remittance` | S |  |
| MKTID.002<br><sub>Main BRD p.71</sub> | Marketing | Remove 'On Hold' tagging so accounts become eligible for extraction | Status updated; appears in extraction; audit | **NEW** | - | Release action on HoldRequest (permission HOLD_REQUEST) | `remittance` | S | OQ24 |
| MKTID.003<br><sub>Main BRD p.71-72</sub> | Marketing | Tag accounts 'On Hold' to exclude them from extraction | Tag available; excluded; logged; only permitted users | **NEW** | - | HoldRequest create (per invoice or list upload) | `remittance` | S | OQ24 |
| MKTID.004<br><sub>Main BRD p.72</sub> | Marketing | Assign approved hold requests to remittance processors | Assignment UI; processor notified; logged; no inactive processors | **CHANGE** | workflow assign + notifications | Assign on OPS_HOLD case | `remittance` | S |  |
| MKTID.005<br><sub>Main BRD p.72-73</sub> | Marketing | Create, cancel and extend hold remittance requests | Role-based actions; status updates; history; no duplicates | **NEW** | - | OPS_HOLD workflow (create / cancel / extend with new hold-until date) | `remittance` | M | OQ24 |
| MKTID.006<br><sub>Main BRD p.73</sub> | Marketing | Approve hold requests and their cancellations | Approval enforced; notifications; logged; cancellation executed after approval | **CHANGE** | workflow + approval inbox source | HOLD_APPROVE permission; PendingApprovalSource for hold requests | `remittance` | S | OQ24 |
| MKTID.007<br><sub>Main BRD p.73-74</sub> | Marketing | Unique reference numbers for hold requests | Non-repeating; searchable; linked | **CONFIGURE** | DocumentNumberService | Series HLD-<yyyy> | `remittance` | S |  |
| MKTID.009<br><sub>Main BRD p.74</sub> | System | Request and process special remittance (claims, renewal, installment due, immediate OR issuance); once validated and approved, tagged Special Remittance, flows into Process Remittance grouped by type, processor, insurer | Invoice Unprocessed or Partially Remitted; paid AR present and applied; check cleared (3-day hold); notifications; accurate reports | **NEW** | - | SpecialRemittanceRequest workflow OPS_SPECIAL_REMIT (request -> approve -> in process -> pushed to Disbursement); creates a special batch through the extraction service | `remittance` | M | OQ25 |

### L. Remittance: tracking, notifications, reports and invoice locking

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| RMTID.032<br><sub>Main BRD p.66</sub> | System | Track hold status and remittance status per account | Visible in account view; timestamped; filterable | **CHANGE** | workflow status history | Both statuses on the invoice panel with history | `opsledger` | S |  |
| RMTID.033<br><sub>Main BRD p.66-67</sub> | System | Notify users on status changes (returned, approved, on hold...) of special remittance requests | Every change notifies with reference and new status; delivery confirmed (e-mail, dashboard) | **CONFIGURE** | Workflow stage-entry notifications; in-app + e-mail outbox with log | Configure stage notifications on OPS_SPECIAL_REMIT and OPS_HOLD | `remittance` | S |  |
| RMTID.034<br><sub>Main BRD p.67</sub> | System | Notify users based on remittance and disbursement status | Timely; logged; users configure preferences / opt out | **CHANGE** | Notifications without per-user preferences | Notification preferences per user and event (messaging extension); disbursement status via DisbursementGateway callbacks | `remittance` | M | OQ02 |
| RMTID.035<br><sub>Main BRD p.67</sub> | System | Notify the Remittance Team of pending negative adjustments | Affected accounts and adjustment details; preferred channel; logged | **NEW** | - | adjustment publishes NegativeAdjustmentPending; remittance notifies REMIT_PROCESS holders (digest) | `remittance` | S |  |
| RMTID.036<br><sub>Main BRD p.67-68</sub> | System | Track remittance status across all stages from extraction to disbursement | Visible and timestamped; history and current stage; consistent across modules | **CHANGE** | workflow history | StageTimeline on batch and invoice | `remittance` | S |  |
| RMTID.037<br><sub>Main BRD p.68</sub> | System | Track estimated items in production reports | Estimated items shown separately with labels | **NEW** | - | 'Estimated' flag on production lines; definition needed | `commission` | S | OQ27 |
| RMTID.039<br><sub>Main BRD p.69,127-129</sub> | System | Generate the Remittance reports listed in the Annex | Expected format and data; save / download / print; authorised view | **NEW** | Report framework | 8 Remittance reports (see REMITTANCE_REPORTS) | `remittance` | M | OQ42 |
| RMTID.040<br><sub>Main BRD p.69-70</sub> | System | Coordinate and sequence invoice adjustments, remittance exemptions and accounting entries with locking, status indicators and approvals | Invoice locked during remittance or while with Comptrollership; statuses (In Remittance, Pending Adjustment, Locked for Editing); other units notified; all actions logged. Negative: concurrent edits, late approvals, duplicate / conflicting actions | **NEW** | Optimistic locking per entity only | InvoiceLock service in opsledger (lock owner module, reason, since; acquire / release in the owner's transaction); status chips on every screen; conflicting actions refused with INVOICE_LOCKED | `opsledger` | M | OQ28 |

### M. Production reconciliation: extraction and file management

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| PRCID.001<br><sub>Main BRD p.79</sub> | System | Automatically extract booked accounts per insurer at a defined frequency; holidays roll to the next working day | Runs at frequency; booked accounts only | **CHANGE** | ManagedJob + holiday calendar | PRODUCTION_EXTRACT job per insurer (frequency table per insurer) over opsledger booked invoices | `prodrecon` | M | OQ29 |
| PRCID.002<br><sub>Main BRD p.79</sub> | System | Lock columns of the extracted file except the insurer 'Remarks' column | Only Remarks editable | **CHANGE** | docgen XLSX; messaging password-protects XLSX | Protected-sheet XLSX with unlocked Remarks column (SheetSpec extension) | `prodrecon` | S |  |
| PRCID.003<br><sub>Main BRD p.79</sub> | System | Generate a cover letter (templated e-mail) for each extract | Includes necessary information | **CONFIGURE** | docgen templates + messaging | Template PRODRECON_COVER_LETTER | `prodrecon` | S | OQ29 |
| PRCID.004<br><sub>Main BRD p.79-80</sub> | System | Auto-assign file name to the extract (convention TBD) | Unique file name; no duplicates | **CONFIGURE** | attachment naming service (W2) | Naming pattern parameter PRODRECON_FILE_PATTERN (default <INSURER>_PRODREG_<yyyyMM>_<seq>) | `prodrecon` | S | OQ29 |
| PRCID.005<br><sub>Main BRD p.80</sub> | System | Display file location, date and time of extract | Metadata visible | **NEW** | - | Extract register (file, stored path / id, created at, by, row count) | `prodrecon` | S |  |
| PRCID.006<br><sub>Main BRD p.80</sub> | System | Generate the file in the agreed format / template | Matches template | **CONFIGURE** | docgen SheetSpec | Template per insurer when needed (default Annex IV Production Register layout) | `prodrecon` | S | OQ29 |
| PRCID.007<br><sub>Main BRD p.80</sub> | Operations | Add a password to the generated file | Password complexity rules; protection confirmed; wrong password cannot open | **FIT** | messaging DocumentProtector (XLSX / PDF passwords, password policy) | Re-use; BDOI password convention parked (BRD-1 Q07) | `prodrecon` | S | OQ29 |
| PRCID.008<br><sub>Main BRD p.81</sub> | System | Send the extracted report to the insurer and store the sent timestamp | Report sent; timestamp logged | **FIT** | messaging outbox with send log | Send from the extract; sentAt from the send log | `prodrecon` | S | OQ29 |
| PRCID.009<br><sub>Main BRD p.81</sub> | System | Receive and automatically upload the insurer production report; separate accounts not in the original extract | Validated and uploaded without manual intervention | **CHANGE** | bulk framework (manual upload) | Bulk handler RECON_INSURER_REPORT; auto-pickup (mailbox / SFTP) parked -> InsurerFileInbox port; lines not in extract go to the 'insurer-only' bucket | `prodrecon` | M | OQ29 |
| PRCID.010<br><sub>Main BRD p.81</sub> | System | Block duplicate upload | Duplicates detected and prevented | **CHANGE** | bulk has no file de-duplication | File SHA-256 + (insurer, period) uniqueness in bulk / recon; attempts recorded (PRCID.032) | `prodrecon` | S |  |
| PRCID.011<br><sub>Main BRD p.81</sub> | Operations | Manually extract the production register for a booking period | Only the selected period | **NEW** | - | Manual extract dialog (insurer(s), booking date range) | `prodrecon` | S |  |

### N. Production reconciliation: production register, viewing and filtering

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| PRCID.012<br><sub>Main BRD p.82</sub> | Operations | View accounts from extracted and uploaded files | All accounts displayed | **NEW** | - | Reconciliation workbench (booked vs insurer lines) | `prodrecon` | M |  |
| PRCID.013<br><sub>Main BRD p.82</sub> | System | Filter and display by production month and insurer | Display matches filter | **NEW** | - | Filters on the workbench | `prodrecon` | S |  |
| PRCID.014<br><sub>Main BRD p.82</sub> | System | Filter by AO / AB for booked and pre-booked | Correct accounts | **NEW** | - | AO / AB from the account's sales unit | `prodrecon` | S |  |
| PRCID.015<br><sub>Main BRD p.82</sub> | System | Edit: add company concerned; add instruction or notation | Edits saved | **NEW** | - | Editable recon fields with audit | `prodrecon` | S |  |
| PRCID.016<br><sub>Main BRD p.82</sub> | Operations | Select company concerned from a list (to be provided) with 'Others' | List available; selection works | **CONFIGURE** | LOV service | LOV RECON_COMPANY_CONCERNED (content from BDOI) | `prodrecon` | S | OQ31 |
| PRCID.017<br><sub>Main BRD p.82-83</sub> | System | Production Register with insurer's feedback | Feedback visible and accurate | **NEW** | Report framework | Report PRC-REGISTER variant INSURER | `prodrecon` | S |  |
| PRCID.018<br><sub>Main BRD p.83</sub> | System | Production Register with insurer and Marketing feedback | Both visible | **NEW** | Report framework | Variants MARKETING / BOTH; Marketing feedback captured on unmatched items | `prodrecon` | S |  |
| PRCID.019<br><sub>Main BRD p.83</sub> | System | List of unbooked accounts and status | Statuses accurate | **NEW** | - | Report PRC-UNBOOKED (insurer-only lines and their resolution status) | `prodrecon` | S |  |
| PRCID.020<br><sub>Main BRD p.83</sub> | Operations | View production register of manually extracted booked accounts | Data matches manual extract | **NEW** | - | Extract detail view | `prodrecon` | S |  |
| PRCID.021<br><sub>Main BRD p.83</sub> | System | Filter by booking date, insurer, BDOI location, market segment, product type | Accurate results across views | **NEW** | - | Shared filter bar | `prodrecon` | S |  |

### O. Production reconciliation: matching, tracking and reports

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| PRCID.022<br><sub>Main BRD p.83-84</sub> | System | Upload matched data from the insurer (policy no., reference / invoice no.); file holds unmatched BDOI lines and insurer production not in the BDOI file (unbooked) | Criteria applied; auto-tag matched; validated and integrated | **NEW** | - | Matching engine run on upload | `prodrecon` | M | OQ30 |
| PRCID.023<br><sub>Main BRD p.84</sub> | System | Match from Booked to Pre-booked stage using the reference number | Auto-tag matched; logged | **NEW** | - | Insurer-only lines matched to pre-booked accounts by ARN / reference (account module) | `prodrecon` | S | OQ30 |
| PRCID.024<br><sub>Main BRD p.84</sub> | System | Automatch pre-booked or no-booking accounts upon booking or upload (frequency blank) | Auto-tag without manual input | **NEW** | - | Listener on opsledger InvoiceBooked + upload trigger | `prodrecon` | S | OQ30 |
| PRCID.025<br><sub>Main BRD p.84</sub> | System | Automatch accounts from uploaded file with specific status at specified time (frequency blank) | Respects status and time | **NEW** | - | RECON_AUTOMATCH ManagedJob with status filter parameter | `prodrecon` | S | OQ30 |
| PRCID.026<br><sub>Main BRD p.84</sub> | System | Consider matched all accounts with a difference of 1.00 and below regardless of BDOI or insurer | Threshold applied; above threshold not matched | **NEW** | - | Tolerance parameter RECON_TOLERANCE = 1.00 per amount field | `prodrecon` | S | OQ30 |
| PRCID.027<br><sub>Main BRD p.84-85</sub> | System | Tag matched when criteria are met: policy no., reference / invoice no., PN no., policy period, commission, basic premium, gross premium, assured name | Criteria applied | **NEW** | - | Key match (policy / invoice) then field comparison; differences within tolerance = MATCHED, else MATCHED_WITH_DISCREPANCY listing the fields | `prodrecon` | M | OQ30 |
| PRCID.028<br><sub>Main BRD p.85</sub> | System | Automate early incentive validation for CLG / CBG motor and fire accounts with 2% premium from insurers (remittance within 30 days) | Incentive only within 30 days. Negative: late remittance, missing insurer data | **NEW** | - | Validation report of incentives claimed vs insurer production (uses remittance EarlyRemittanceIncentive) | `prodrecon` | S | OQ23 |
| PRCID.029<br><sub>Main BRD p.85</sub> | Operations | View history details | Timestamps, actions, user | **FIT** | Audit trail + workflow history | History tab | `prodrecon` | S |  |
| PRCID.030<br><sub>Main BRD p.85</sub> | Operations | Tag and track status: Matched, Matched with discrepancies, Unmatched pre-booked, Unmatched no booking | Visible and logged; discrepancies visible; separate bucket per status | **NEW** | - | ReconStatus enum + bucket tabs | `prodrecon` | S |  |
| PRCID.031<br><sub>Main BRD p.85-86</sub> | System | Monitor upload status (successful / unsuccessful) | Displayed correctly | **FIT** | bulk job statuses | Re-use | `prodrecon` | S |  |
| PRCID.032<br><sub>Main BRD p.86</sub> | System | Record number of upload attempts and status | Timestamp, user, status per attempt | **CHANGE** | bulk_job per upload | Attempt counter per (insurer, period) incl. blocked duplicates | `prodrecon` | S |  |
| PRCID.033<br><sub>Main BRD p.86</sub> | System | Repository of unmatched and duplicate accounts | Searchable and filterable | **NEW** | - | Unmatched / duplicate repository view with disposition and feedback | `prodrecon` | S | OQ31 |
| PRCID.034<br><sub>Main BRD p.86</sub> | System | Report of item count and extraction timestamp | Accurate count and timestamp | **NEW** | Report framework | Report PRC-EXTRACT-LOG | `prodrecon` | S |  |
| PRCID.035<br><sub>Main BRD p.86</sub> | System | Production Reconciliation Summary Report | Matched / unmatched summary accurate | **NEW** | Report framework | Report PRC-SUMMARY (Annex IV #1) | `prodrecon` | S |  |
| PRCID.036<br><sub>Main BRD p.86-87</sub> | System | Summary of unmatched accounts per location | Branch mapping accurate | **NEW** | Report framework | Report PRC-UNMATCHED-LOC (Annex IV #2) | `prodrecon` | S |  |
| PRCID.037<br><sub>Main BRD p.87</sub> | System | Summary of unmatched accounts per Marketing AO / AB (booked and pre-booked only) | Grouping by AO / AB correct | **NEW** | Report framework | Report PRC-UNMATCHED-AO (Annex IV #3) | `prodrecon` | S |  |
| PRCID.038<br><sub>Main BRD p.87</sub> | System | Report of unmatched accounts with feedback and disposition | Exportable, accurate | **NEW** | Report framework | Report PRC-UNMATCHED-FEEDBACK | `prodrecon` | S | OQ31 |
| PRCID.039<br><sub>Main BRD p.87</sub> | System | Summary report per disposition | Disposition categories correct | **NEW** | Report framework | Report PRC-DISPOSITION (Annex IV #4) | `prodrecon` | S | OQ31 |

### P. Adjustment / cancellation: endorsement transactions

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| ADJID.001<br><sub>Main BRD p.89</sub> | System | Single or multiple financial endorsement transactions for booked accounts (types: financial, non-financial, internal adjustment); no TSI change together with partial cancellation; no submission while on the remittance queue | Select one or many accounts; financial types available; saved to account history; confirmation. Negative: not booked, incompatible types, missing fields, account in remittance queue | **CHANGE** | underwriting endorsements (insurer model); booking module (W3) posts NB endorsements and cancellations | EndorsementRequest (single / multi-account batch) in adjustment; posting delegated to booking's posting contract; guards: booked, INVOICE_LOCKED (in remittance), incompatible type combinations | `adjustment` | L | OQ32 |
| ADJID.002<br><sub>Main BRD p.89-90</sub> | Operations | Select financial endorsement type: change of TSI (+/-), change in insured items / persons / units / locations, change in commission rate, change in premium rate or amount, extension of cover (period), change of cover, adjustment in charges (DST, premium tax / VAT, LGT, others), minimal balance | All types in dropdown; selection drives input fields; processed by type | **NEW** | - | LOV ENDORSEMENT_TYPE (financial / non-financial / internal) with attribute 'input form' and 'sign' | `adjustment` | S | OQ35 |
| ADJID.003<br><sub>Main BRD p.90</sub> | System | Single / multiple non-financial endorsements for booked accounts | Select accounts and type; details updated without financials; history updated. Negative: financial change under non-financial, financial entries generated | **CHANGE** | underwriting non-financial endorsements | Non-financial request updates the account through AccountService (risk / assured data) with no accounting event | `adjustment` | M | OQ32 |
| ADJID.004<br><sub>Main BRD p.90-91</sub> | System | Non-financial types: descriptive changes; change of period cover (inception moved, term unchanged); extension of period covered (no financial impact); extension of cover (clauses, no premium); change of assured name / information; internal adjustment without insurer endorsement; Marketing updates the account | Each type selectable and accepts flow-in; no financial effect; internal adjustments logged without insurer. Negative: invalid date range | **NEW** | - | Types seeded in ENDORSEMENT_TYPE; date validations | `adjustment` | S | OQ35 |
| ADJID.005<br><sub>Main BRD p.91-92</sub> | System | Return transactions before batch posting (not qualified), optional reason, excluded and flagged | Return option at batch validation; multi-select; excluded; logged; summary with reasons | **NEW** | - | Posting batch review step with return action | `adjustment` | S |  |
| ADJID.006<br><sub>Main BRD p.92</sub> | System | Batch posting of cancellations and adjustments | Batch upload; per-transaction validation; post-batch summary; no duplicates on re-upload | **CHANGE** | bulk framework | AdjustmentPostingBatch + bulk handler ADJ_BATCH; idempotent on request no. | `adjustment` | M |  |
| ADJID.007<br><sub>Main BRD p.92</sub> | System | Return problematic cancellation / adjustment requests with mandatory reason; requester notified | Reason mandatory; logged; notification; processed requests cannot be returned | **CHANGE** | workflow return with reason + notification | Transition return on OPS_ENDORSEMENT | `adjustment` | S |  |
| ADJID.008<br><sub>Main BRD p.92-93</sub> | System | Increase of TSI: check package TSI limit (prompt Marketing AO to prepare a quotation), add insurer for TSI above capacity, recompute non-package shares on endorsed sum insured, premium from added TSI and period; no TSI change with partial cancellation | Validated vs package limits; quotation prompt; add insurer; recompute shares and premium. Negative: no prompt, overwrite without backup | **NEW** | catalog package limits (W2); quotation module (W3) | Limit check via catalog; 'requires quotation' outcome creates a quotation request link (quotation module); co-insurance share recompute; versions keep original values | `adjustment` | L | OQ33 |
| ADJID.009<br><sub>Main BRD p.93-94</sub> | System | Decrease of TSI: apply excess payment to another invoice or refund; refund entry linked to unapplied module; premium decreases with remaining TSI and period | Recalculated; excess applied or refunded; linked to unapplied. Negative: excess not detected | **NEW** | - | Excess from recompute handed to cashiering UnappliedPayment (disposition apply / refund) | `adjustment` | M | OQ36 |
| ADJID.010<br><sub>Main BRD p.94</sub> | System | Extension of cover with additional premium subject to approval | New dates; additional premium; approval before finalising | **NEW** | - | Approval stage in OPS_ENDORSEMENT for extension types | `adjustment` | S | OQ35 |

### Q. Adjustment / cancellation: computation and accounting

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| ADJID.011<br><sub>Main BRD p.94</sub> | System | Accounting entries for booked accounts per financial endorsement type (default entries defined by Comptrollership) | Each type maps to entries; correct GL; audit. Negative: unmapped type, inactive GL, duplicate entries | **CHANGE** | Accounting engine: event -> rule -> journal, idempotent source reference | Financial endorsements and cancellations (flat / flat retain DST / partial) posted through booking's EndorsementPostingService (one booking event type per class); adjustment adds OPS_AR_INSURER_SETUP and payment re-application; rules by Comptrollership | `adjustment` | M | OQ07 |
| ADJID.012<br><sub>Main BRD p.94-95</sub> | System | Set up excess payment resulting from cancellation and adjustment | Calculated; tagged for disposition; linked; units notified. Negative: not recorded, mis-applied | **NEW** | - | cashiering UnappliedService.createFromAdjustment(...) (AP Unapplied Payments); AR Insurer for remitted amounts (OPS_AR_INSURER) | `adjustment` | M | OQ07 |
| ADJID.013<br><sub>Main BRD p.95</sub> | System | Reverse applied payments per latest premium and route excess / unapplied to Marketing disposition | Premium recalculated; application adjusted; excess routed; history. Negative: locked period, missing rules, wrong premium | **NEW** | - | cashiering ApplicationService.reapply(invoice) after posting; excess -> disposition queue | `adjustment` | M | OQ15 |
| ADJID.014<br><sub>Main BRD p.95-96 + UPDATED by Addendum 1 p.5</sub> | System | ADDENDUM: compute / re-compute premium, commission, refund premium and sum insured per insurer by endorsement type (change / increase of TSI, extension, rate change); if income or commission is affected generate and link a Service Invoice; log all changes | Triggered by endorsement; updated values; breakdown per insurer; service invoice when income / commission affected; audit. Negative: wrong formula, missing insurer configuration, overwrite without backup | **CHANGE** | catalog PremiumCalculator (W2); booking service invoice (W3) | Recompute via catalog calculator per insurer share; before / after versions stored; service invoice (or credit) through booking's ServiceInvoiceService when commission / income delta <> 0 | `adjustment` | L | OQ34,OQ36 |
| ADJID.026<br><sub>Main BRD p.101-102</sub> | System | Receive a file of accounts with minimal balances from 10.00 to 100.00; write off or credit per rules; match invoice numbers with PR balances to zero out or credit; summary report | CSV / Excel parsed; only 10.00-100.00; action per configuration; logged; invoice + PR matched; unmatched flagged. Negative: bad format, out-of-range amounts, missing invoices, misconfigured rules, duplicates | **NEW** | - | Bulk handler MINIMAL_BALANCE_FILE with rule table (range, action WRITE_OFF / CREDIT, GL); event OPS_WRITE_OFF; sets WRITTEN_OFF flag | `adjustment` | M | OQ11 |
| ADJID.027<br><sub>Main BRD p.102-103</sub> | System | Allocate adjustment amounts per insurer percentage share (co-insurance); payment allocation per share done in Cashiering; one invoice with internal distribution; log per insurer | Split per configured share; one invoice; adjustments follow original shares; log. Negative: 100% to lead insurer | **NEW** | - | InsurerShare lines on the opsledger invoice; allocation helper used by cashiering application and adjustment postings | `opsledger` | M | OQ33,OQ50 |
| ADJID.028<br><sub>Main BRD p.103</sub> | System | Control over-adjustment: baseline limit, cumulative tracking vs original premium / DTIP, prompt on exceed or negative balance, block duplicates, review previous adjustments | Prompt on exceed; duplicates blocked; review before applying; automatic totals vs baseline | **NEW** | - | Cumulative adjustment ledger per invoice (opsledger) + guard in adjustment; baseline parameter | `adjustment` | S | OQ37 |

### R. Adjustment / cancellation: documents, reports, traceability

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| ADJID.015<br><sub>Main BRD p.96</sub> | User (Marketing) | Generate an Endorsement Slip for external endorsements (not for internal adjustments) - noted as a Marketing activity | Required fields (Annex V); download / print; unique slip number | **NEW** | docgen | Endorsement Slip template (Annex V fields), series ES-<yyyy>; same function serves MKTID.008 | `adjustment` | S | OQ32 |
| ADJID.016<br><sub>Main BRD p.96</sub> | System | Adjustment and Daily Endorsement Reports | All transactions of the day; filter by date, type, user; exportable; on schedule | **NEW** | Report framework + ManagedJob | Report ADJ-DAILY (scheduled daily run archived) | `adjustment` | S |  |
| ADJID.017<br><sub>Main BRD p.97</sub> | System | Validation List of posted transactions with GL entries and adjustment remarks (type, invoice, endorsement ref / request no., segment, AO, policy, assured, insurer, GL code, Dr/Cr, reason, validation date, batch no.) | All fields; accurate; exportable / printable; matches accounting | **NEW** | Journal lines queryable by source reference | Report ADJ-VALIDATION-LIST joining requests with gl_ledger_entry by source reference | `adjustment` | M |  |
| ADJID.018<br><sub>Main BRD p.97-98</sub> | System | Validation Slip summarising the endorsement transaction | Summary and validation status; download / print; linked; not for unvalidated | **NEW** | docgen | Validation Slip PDF per request | `adjustment` | S |  |
| ADJID.019<br><sub>Main BRD p.98</sub> | System | Adjustment Report | Type, amount, reason; filter by date, account, segment, AO, risk type; exportable | **NEW** | Report framework | Report ADJ-REGISTER | `adjustment` | S |  |
| ADJID.020<br><sub>Main BRD p.98</sub> | System | Link endorsement transactions to the original account reference number | Original reference shown; all endorsements traceable from the account; link kept across endorsements | **FIT** | ARN carried on every record (BRD-1 BRNB.102) | Store ARN + invoice no. on requests | `adjustment` | S |  |
| ADJID.021<br><sub>Main BRD p.98-99</sub> | System | Aging of transactions from request date to completion date | Aging computed; sortable / filterable | **CHANGE** | workflow SLA / stage timers | Aging columns + report ADJ-AGING | `adjustment` | S |  |
| ADJID.022<br><sub>Main BRD p.99</sub> | System | Record and view endorsement transactions (history, old and new details) | Complete, searchable history with status and remarks | **CHANGE** | Audit trail with before / after | Before / after snapshot per request | `adjustment` | S |  |
| ADJID.023<br><sub>Main BRD p.99</sub> | System | Detect duplicate endorsement requests (endorsement ref no., reason, request type; summary adds invoice, policy, assured) and notify; proceed / cancel / override with justification | Detection; warning; override with justification | **NEW** | - | Duplicate check service with override reason (audited) | `adjustment` | S |  |
| ADJID.024<br><sub>Main BRD p.100</sub> | System | Search by Account Reference Number to pull up policy details incl. payment history and remittance | Accurate; payment and remittance history; fast; partial match and auto-complete | **CHANGE** | Account search (W2) | ARN auto-complete + invoice 360 panel (opsledger) | `adjustment` | S |  |
| ADJID.025<br><sub>Main BRD p.100</sub> | System | Attach supporting documents to endorsement requests (document list blank in BRD) | Supported types accepted; linked; confirmation. Negative: size / format, corrupted | **FIT** | attachment module (types, size, checksum, multi-file) | Document types LOV for endorsement documents | `adjustment` | S | OQ32 |
| MKTID.008<br><sub>Main BRD p.114</sub> | Marketing | Generate an Endorsement Slip for external endorsements (none for internal adjustment) | Required fields; download / print; unique slip number | **NEW** | docgen | Same function as ADJID.015 exposed to Marketing (permission ENDT_SLIP) | `adjustment` | S | OQ32 |

### S. Collection of commission receivables (Direct Payment) and incentives

| BR ID | Persona | Requirement | Key acceptance criteria | Fit | Current | Proposed solution | Module | Effort | Q |
|---|---|---|---|---|---|---|---|---|---|
| CMRID.001<br><sub>Main BRD p.106</sub> | System | Access the files where Marketing Collection HO and branches save their DP lists (predefined naming, e.g. BranchName_DP_YYYYMMDD); DP tagging flows into the Commission Receivable module; consolidated HO + branch list as billing basis | Each HO / branch uploads on schedule; naming followed; consolidation without duplicates. Negative: missing submissions, naming errors, cross-branch duplicates | **NEW** | - | DpList intake: bulk handler DP_LIST + CollectionFeed port; submission tracker per branch and period; consolidation with de-duplication by invoice / policy | `commission` | M | OQ38 |
| CMRID.002<br><sub>Main BRD p.106-107</sub> | System | Match DP entries against production reports and historical commission trackers; confirm full premium remittance, unpaid commission, completeness; check adjustments at invoice level | 100% checked (fully paid to insurer, commission status, completeness); discrepancies flagged. Negative: unpaid premium passes, commission paid twice | **NEW** | - | DpValidation over opsledger (booked invoice, DP flag, adjustments) and commission receivable history | `commission` | M | OQ38 |
| CMRID.003<br><sub>Main BRD p.107</sub> | System | Validate production data to exclude negative amounts and erroneous bookings from incentive eligibility | Exclusion criteria defined; flagged and excluded; alerts for review | **NEW** | - | Exclusion rule table + INCENTIVE_EXCLUSION alert | `commission` | S | OQ39 |
| CMRID.004<br><sub>Main BRD p.108</sub> | System | Commission receivable reports separating DP / Direct Bill from regular commissions; partial remittances and net commission | DP vs regular; net after partial remittance; on schedule | **NEW** | Report framework | Report CMR-COMMISSION-RECEIVABLE | `commission` | S | OQ42 |
| CMRID.005<br><sub>Main BRD p.108-109</sub> | System | Calculate and validate No Touch and Top Up incentives for Retail and Corporate accounts from production targets, rates and multipliers per period (e.g. June-December) | Correct rates / multipliers; accurate values. Negative: misapplied multipliers, missing data | **NEW** | - | IncentiveScheme engine (scheme, period, target tiers, rate, multiplier, segment) - rule table empty until BDOI gives the schemes | `commission` | L | OQ39 |
| CMRID.006<br><sub>Main BRD p.109</sub> | System | Motor Mania incentive: fixed amount per policy (e.g. PHP 500 / 1,000) by minimum basic premium (e.g. 10,000 / 50,000); quarterly moving to yearly; pass-on for branches | Minimum premium validated; correct amount. Negative: policies below minimum | **NEW** | - | Scheme type FIXED_PER_POLICY with thresholds and period type QUARTERLY / ANNUAL; branch distribution statement | `commission` | M | OQ39 |
| CMRID.007<br><sub>Main BRD p.109</sub> | System | Automate commission receivable computation from raw files and insurer data | No manual intervention | **NEW** | - | CommissionReceivable per DP invoice (commission, VAT, WTAX, net) computed from opsledger + catalog commission rates | `commission` | M | OQ38 |
| CMRID.008<br><sub>Main BRD p.109-110</sub> | System | Tag DP accounts; auto-tag valid / invalid (e.g. 'DP for billing', 'DP for confirmation'); audit of rejected accounts; dropdown reasons for insurer feedback | Auto-tags; audit of rejected / returned; reasons used consistently | **NEW** | - | DP status on DpItem; LOV DP_FEEDBACK_REASON | `commission` | S | OQ40 |
| CMRID.009<br><sub>Main BRD p.110-111</sub> | System | Sort validated accounts by insurer and assign handlers for billing; insurer approves or rejects with reason | Billing within SLA; files password-protected; responses logged; upload insurer file; valid to payment, invalid returned; returned accounts flow to Collection with timestamp and reason | **NEW** | messaging protected XLSX; workflow assignment | CommissionBilling per insurer (workflow OPS_DP_BILLING), billing file, bulk handler DP_INSURER_RESPONSE, CollectionFeed outbound for returns | `commission` | L | OQ38,OQ40 |
| CMRID.010<br><sub>Main BRD p.111</sub> | System | Track OR numbers and amounts, BIR certificate submissions, withholding tax and all insurer feedback | Logged; certificates tracked; feedback reportable | **NEW** | tax module Certificate2307 (as payor) | Links commission collection OR (cashiering), WTAX and certificate receipt to the billing line | `commission` | S | OQ41 |
| CMRID.011<br><sub>Main BRD p.111</sub> | System | Feedback timeline: flag insurer feedback delays beyond 10 working days | Delays flagged | **CHANGE** | workflow SLA hours; alert framework; holiday calendar | Working-day SLA on OPS_DP_BILLING 'awaiting insurer' stage + DP_FEEDBACK_OVERDUE alert | `commission` | S | OQ40 |
| CMRID.012<br><sub>Main BRD p.111-112</sub> | System | Automate coordination with insurers: send validation requests, track responses | Requests logged; responses received systematically | **CHANGE** | messaging outbox with log | Validation request e-mail per insurer with protected file; response via upload | `commission` | S | OQ06 |
| CMRID.013<br><sub>Main BRD p.112</sub> | System | Sanitise data: remove duplicate, invalid, incomplete accounts; only confirmed fully paid DPs billed | Sanitation criteria remove invalid; confirmation verifies payment before billing | **NEW** | - | Sanitation step with rule list and reviewer confirmation | `commission` | S | OQ38 |
| CMRID.014<br><sub>Main BRD p.112-113</sub> | System | Production reports per branch and insurer with consolidated yearly data (total production, estimated items, performance vs targets) | Total production included; no missing branches / insurers | **NEW** | Report framework | Report CMR-PRODUCTION-YEARLY | `commission` | S | OQ27,OQ42 |
| CMRID.015<br><sub>Main BRD p.113</sub> | System | Tag BIR withholding tax certificates from insurers to ORs issued by the cashier and submit to Comptrollership digitally; acknowledgment tracking; scanned copies | Select ORs; tag certificates; submit; log dates and acknowledgment; Comptrollership views / acknowledges / rejects with reason; resubmission. Negative: no OR linked | **NEW** | attachments; approval inbox | CertificateSubmission (OR links, certificate period / amount, scans) with workflow OPS_BIR_CERT (SUBMITTED -> ACKNOWLEDGED / REJECTED); permission BIR_CERT_ACK for Comptrollership | `commission` | M | OQ41 |
| MKTID.011<br><sub>Main BRD p.75</sub> | System | Tag DP (Direct Payment) during quotation creation and conversion to policy; reflected in invoice and payment modules | DP checkbox at quotation / policy creation; invoice shows DP status; payments matched to the right invoice | **CHANGE** | BRD-1 BRNB.114 payment arrangement DIRECT_TO_INSURER on the account (W2) | Carry the flag to the opsledger invoice; quotation must expose the same flag (W3) | `opsledger` | S | OQ38 |
| MKTID.012<br><sub>Main BRD p.75-76</sub> | System | Auto-reverse DP premium receivable only after valid DP tagging by Marketing, flow into Commission Receivable and confirmed collection with uploaded schedule; auto-match commission / premium portion; no manual files | Tag validated first; auto-reverse on valid tag + confirmed collection; logged; missing / invalid tag blocks and notifies. Negative: incorrect DP tagging | **NEW** | - | DP reversal service in commission: event OPS_DP_PR_REVERSAL (Dr DTIP / Cr PR by component) on collection confirmation; guarded by tag validation | `commission` | M | OQ07,OQ38 |

## 6. Lists and formats from the BRD

### 6.1 Receipt types

| Receipt | Class | Payments |
|---|---|---|
| AR | Premium payments | Bills Payment, OTC, Trade, CLPC, PDC, Direct Credit |
| AR | Non-premium payments (insurance payments) | Refund, Other expenses, AR Insurance |
| OR (HO only) | Service Fee Income (e.g. risk management fee, consultancy fee) | - |
| OR (HO only) | Insurance Profit Share | - |
| OR (HO only) | Commissions (with VAT / withholding tax) | - |
| OR (HO only) | Incentives | - |
| OR (HO only) | Others (discount) | - |

### 6.2 Cancellation and reinstatement reasons (CSHID.003 / 004 / 005)

| List | Group | Values |
|---|---|---|
| RECEIPT_CANCEL_REASON | PREMIUM | Discrepancy in check amount; No signature; Incorrect payee; Check not picked-up; Incorrect check details provided by the client or Insurer; Others (specify) |
| RECEIPT_CANCEL_REASON | COMMISSION | Incorrect payment details; Negative VAT; Discount not applied |
| RECEIPT_CANCEL_REASON | PDC | PDC retrieval / replacement - not yet due: for replacement; PDC retrieval / replacement - not yet due: pull out |
| RECEIPT_CANCEL_REASON | GENERAL | Bounced or returned check (reinstatement needs no form); Error in issuance details; Error printing; Check not picked up; Double issuance; Others (specify) |
| REINSTATEMENT_REASON | PREMIUM | Due to cancellation; Mis-application of payment; Withholding tax adjustment; Others (specify) |
| REINSTATEMENT_REASON | DIRECT_PAYMENT | Due to double reversal; Due to wrong details of OR; Withholding tax adjustment; Due to cancellation; Others (specify) |
| Reinstatement fields | PREMIUM | Invoice number; Acknowledgment Receipt number; Assured / Payor name; Amount reinstated; Account officer; Unit Head; Team Leader |
| Reinstatement fields | DIRECT_PAYMENT | Invoice number; OR number; Assured / Payor name; Amount reinstated |

### 6.3 Payment files (CSHID.008)

| Handler | Source | Type | Fields |
|---|---|---|---|
| `PAY_BILLS` | Bills Payment - IT-DCO | TXT (FS01) | Incremental #, Invoice #, Assured's name, Amount, Late deposit (Y/N), Phone #, Branch code, Date of payment, Time of payment |
| `PAY_TRADE` | Trade Payment - CIB | TXT | Transaction date, Branch name, Transaction description, Debit, Credit, Running balance, Check no. |
| `PAY_CLPC` | CLPC Payment | Excel | Count, Date credit, Amount, AR number, Invoice, Assured, PN number, Corp. dept, Risk code, Remarks |
| `PAY_PDC` | PDC Payment (from PMS) | Excel | (layout not given - 'PDC from PMS') |
| `PAY_DIRECT_CREDIT` | Direct Credit | TXT (FS01/04) | Transaction date, BP filename, Transaction no, Paid amount, Payment type, Payor, Account ref no, Assured, EBIX_RefNo, Logged by, Requestor |

### 6.4 Application, matching and disposition

- Application hierarchy (CSHID.022): Premium Receivable - DST -> Premium Receivable - Premium Tax (VAT) -> Premium Receivable - LGT -> Premium Receivable - Other Charges -> Premium Receivable - Basic Premium; mode of payment: Check, Cash.
- Matching categories (CSHID.020): **APPLIED** Matched on booked account with outstanding PR (100%, or 98% for 2% CWT accounts); **PREBOOKED** Matched on a pre-booked account; waits for booking; re-run manual / scheduled; **UNAPPLIED_NO_MATCH** No match on booked or pre-booked accounts; **EXCESS** Payment exceeds expected / invoiced amount (excess unapplied); **CANCELLED_REFERENCE** Matched against a cancelled booking / slip (client used an invalid reference).
- Disposition types (CSHID.024): Apply to other invoice, DST payment application, Refund, Reclass, Transfer to other marketing unit, Others (customizable). Tabs: Unapplied, Monitoring, For Approval, For Reversal.
- Minimal balance rules:
  - CSHID.016: Premium receivable balance <= PHP 10.00 -> Auto-reverse unless equal to 2% CWT, DST charged or the entire premium (compared with PR2307 balance)
  - KC 5.f (Cashiering summary): Records with minimal balances <= PHP 10.00 -> Auto-reverse to AP overages
  - ADJID.026: Balances >= 10.00 and <= 100.00 (file) -> Write-off or credit per configured rules; match invoice to PR balance
  - PRCID.026: Recon difference <= 1.00 -> Treated as matched

### 6.5 Remittance

- Remittance types: With Incentives, Normal - Dollar, Normal - Peso.
- Extraction tags (RMTID.003): EXTRACTED (Met criteria and included in the extract); UNEXTRACTED_NOT_DUE (Did not meet criteria and not yet due for remittance); UNEXTRACTED_DUE (Did not meet criteria but due for remittance); RETURNED (Met criteria, extracted and returned by user).

| Remittance status (RMTID.019) | Description |
|---|---|
| APPROVED | Approved and forwarded to the Disbursement module; no longer editable |
| REVIEW_IN_PROCESS | Being reviewed by the Remittance Processor |
| REQUESTED_FOR_HOLD | Pending hold request; linked to Hold Remittance tab |
| UNAPPLIED_PAYMENT | Payment received but not yet applied |
| WITH_OUTSTANDING_BALANCE | Uncollected premium; see Cashiering |
| UNPROCESSED | Not yet in any extract (pending adjustments, unapplied payments or not yet due) |
| FULLY_REMITTED | Paid in full, DV number assigned, no outstanding DTIP |
| PARTIALLY_REMITTED | Partial payment processed, DV number assigned, DTIP balance remains |

### 6.6 Production reconciliation

- Match fields (PRCID.027): Policy Number, Reference / Invoice Number, PN No., Policy Period, Commission Amount, Basic Premium, Gross Premium, Assured Name; tolerance 1.00 (PRCID.026).
- Statuses (PRCID.030): Matched, Matched with discrepancies, Unmatched pre-booked, Unmatched no booking.

### 6.7 Endorsements (ADJID.002 / 004, Annex V)

- Financial types: Change of Total Sum Insured (increase or decrease); Change in insured items, persons / members, units, locations; Change in commission rate (increase or decrease); Change in premium rate (increase or decrease) or premium amount; Extension of cover (period of cover); Change of cover; Adjustment in charges (DST, premium tax / VAT, LGT, others) - increase or decrease; Minimal balance.
- Non-financial types: Descriptive changes (assured, person, risk / item, perils, coverage, extensions, warranties, clauses, deductibles); Change of period cover (inception moved earlier / later, term unchanged); Extension of period covered (inception and expiry moved, no financial impact); Extension of cover (extensions / clauses / warranties / deductibles added, no premium); Change of assured name or information (address, birthdate, occupation); Internal adjustment (no insurer endorsement needed).
- Request types (Annex V): Flat Cancellation; Flat Cancellation - Retain DST; Partial Cancellation; Increase / Decrease in TSI; Increase / Decrease of Premium Rate; VAT / Premium Tax exempt; Increase / Decrease of taxes (LGT, FST, other taxes); Change of Cover; Extension of Cover; Write-off; Decrease / Increase in Commission; Cancellation Reversal.
- Reasons for cancellation (Annex V, 33): Dollar Booking; Breach Rate; Deletion of employees; Deletion of units; Deletion of vehicle; With existing insurance; Client submitted own policy; Replaced by another policy; Due to non-payment; Egressed Tenant; Due to store closure; Double issuance; Double booking; Unit sold; Unit junked; Unit unserviceable; Unit cannot be located; Unit repossessed; Unit surrendered; Project or transaction did not materialize; Loan fully paid with other BDO Units; Correction of premium; Due to incorrect policy details; Not for issuance; To correct booking; Accounts transferred to RMU; Accounts transferred to other unit; Case terminated (RMU); Case decided; Case dismissed; Due to non-payment or difficult to collect; Auto cancel by insurer due to nonpayment; Others: Specify.
- Endorsement slip fields (Annex V): Date; Insurer; Co-insurer/s; Endorsement Request No.; Invoice No.; Request Type; Reason for cancellation; Effective Date of Cancellation / Endorsement; Additional / Other Instructions; Assured; Risk Code; Risk Description; Policy No.; Period of Cover; Marketing AO; Market Segment; Total Sum Insured with the breakdown; Premium Rate; Payment Status: outstanding, fully paid or partially paid (amount paid, date, remaining AR); Remittance Status; Approved by: Marketing TL.
- Validation list fields (ADJID.017): Type of cancellation / adjustment (external / internal / financial / non-financial); Invoice No.; Endorsement Reference No.; Endorsement Request No.; Market Segment; Requesting AO; Policy No.; Assured Name; Insurer; GL Code; Debit / credit entries; Reason for cancellation (complete remarks); Validation date; Validation Batch Number.

### 6.8 Direct payment and incentives

- DP tags / statuses: DP for confirmation -> DP for billing -> Billed -> Approved by insurer -> Rejected by insurer (returned to Collection) -> Collected (OR issued) -> PR reversed.

| Scheme | Name | Beneficiary | Rule as written | BRD |
|---|---|---|---|---|
| NO_TOUCH | No Touch | Retail / Corporate (CBG motor per KC 5.a) | Production target tiers with rates and multipliers per period (e.g. June-December) | CMRID.005 |
| TOP_UP | Top Up | Retail / Corporate | Production target tiers with rates and multipliers per period | CMRID.005 |
| MOTOR_MANIA | Motor Mania | Branches (pass-on) | Fixed amount per policy (e.g. PHP 500 / 1,000) by minimum basic premium (e.g. 10,000 / 50,000); quarterly moving to yearly; SLA-bound, renegotiable | CMRID.006 |
| EARLY_REMITTANCE | Early remittance incentive | Insurer -> BDOI | Additional incentive (2% of premium for CLG / CBG motor and fire) when remitted within 30 days from inception | RMTID.023, PRCID.028 |

## 7. Reports

### Cashiering (Annex II)

| # | Report | Fields as given |
|---|---|---|
| 1 | Applied Premium Reports | AR No., Date, Amount Paid, Invoice No., Payor / Client Name, Risk Code, Count, Total Amount (PDF, Excel) |
| 2 | Applied Commission Reports | OR Number, Invoice Number, Number of Applied Invoices, Insurance Company, Basic Commission, W/TAX, EVAT, Net Commission, Total amount applied |
| 3 | Post-dated Checks Warehousing | AR Date, AR Number, Maturity Date, Client Name, Amount, Bank Code, Branch, Check Number, Market Segment |
| 4 | Minimal Balance of Unapplied Payments (Excess Payments) | AR Number, AR Date, Market Unit, Section Unit, Minimal Amount, Count, Total Amount |
| 5 | Cancelled Official Receipts | Date Issued, OR Number, Assured, Gross Amount, VAT, WTAX, Amount, Total Amount |
| 6 | Cancelled Acknowledgment Receipts | Date Issued, Reason for Cancellation, AR Number, Assured, Gross Amount, VAT, WTAX, Amount, Total Amount |
| 7 | Check Pick-Up Request Reports | Date of Request, Time of Request, AR Number, Date Issued, Amount, Assured's Name, Collections Handler / Requestor, Count, Total Amount |
| 8 | Priority Posted Accounts | OR Number, Payor, Amount Paid, Date Paid, Encoder, Branch, Status, Invoice Number, Policy Number, Risk Code, Corporate Depart |
| 9 | Unapplied Commission Receivable Extract for Mancom Reports | _not specified (OQ42)_ |
| 10 | Unapplied Commission Receivable Payments YTD balance per Criteria | _not specified (OQ42)_ |
| 11 | Premium Minimal Balance | _not specified (OQ42)_ |
| 12 | Commission Minimal Balance | _not specified (OQ42)_ |
| 13 | Daily Cash Reconciliation Reports | _not specified (OQ42)_ |
| 14 | Advance Payment Transaction (Auto-Credit) | _not specified (OQ42)_ |
| 15 | Payment Reversal Filters | _not specified (OQ42)_ |
| 16 | Direct Payment | _not specified (OQ42)_ |
| 17 | Reinstatement Monitoring Report | _not specified (OQ42)_ |
| 18 | Re-Application Report | _not specified (OQ42)_ |
| 19 | Certification of Payment | AR Number, Policy Number, Requesting Market Unit, Date (COP) issued, Count |
| 20 | Reinstatement | _not specified (OQ42)_ |
| 21 | CWT | _not specified (OQ42)_ |

### Remittance (Annex III)

| # | Report | Fields as given |
|---|---|---|
| 1 | Remittance Tracker | Insurance Company, Remit Type, Batch no., No. of Accounts Extracted, Net Due, Incentive Amount, Due Date (PDF, Excel) |
| 2 | Special Remittance Register | Invoice Number, Policy Number, Insurance Company, Name of Assured, Reason, Processor, Requestor, Marketing Segment, Date request posted, Date pushed to Disbursement, Processed Age, Checking Age, Approval Age |
| 3 | Remittance Schedule Normal | Invoice Number, Policy Number, Endorsement Number, With COI, Name of Assured, Risk Code, Record Status, Date Last paid, Date reversed, Date Inception, Date Booked, Date Expiry, OR Delivery, Paid AR, Realized Commission, Realized VAT, WTAX, DTIP, Net Due, Mall Assurance (Mode of Payment, Service Charged, Credit/Debit card Charged, Total Deductions, VAT, WTAX), Net Due after Mall Assurance, Date of Original Receipt, Official receipt Number, OR Amount |
| 4 | Remittance Schedule Special | As Normal without Mall Assurance columns: ... Paid AR, Realized Commission, Realized VAT, WTAX, DTIP, Net Due, Date of Original Receipt, Official receipt Number, OR Amount |
| 5 | DTIP Status Report - Summary | Insurance Company, No. of Accounts, Amount, Remittance Status |
| 6 | DTIP Status Report - Detailed | Invoice Number, Name of Assured, Inception Date, Date of Expiry, Insurance Company, Risk Code, Product Type, Basic premium, AR Paid, AP / For Remittance, Commission Collected, CAT Collected, Date of last Remittance, Date of Last Payment, Aging from Booking Inception, Aging from last payment to Inception, Outstanding DTIP, Outstanding RA, Uncollected Commission, Uncollected VAT |
| 7 | Remittance Schedule with Incentives | Invoice Number, Policy / Endorsement Number, With COI, Name of Assured, Risk Code, Record Status, Date Last paid, Date reversed, Date Inception, Date Booked, Date of Incentive Expiry, Paid AR, Realized Commission, Realized VAT, WTAX, DTIP, Net Due Before Incentives, Basic Premium |
| 8 | List of Remitted Accounts with Batch Number | _not specified (OQ42)_ |

### Production reconciliation (Annex IV)

| # | Report | Fields as given |
|---|---|---|
| 1 | Production Reconciliation Summary Report | Insurance Company, Matched Item, Matched Amount, Matched with Discrepancies Item / Amount, Unmatched Item / Amount, Remarks |
| 2 | Summary Report For Unmatched Accounts Per Location | Insurance Company, Disposition, Location, Items, Amount |
| 3 | Summary Report For Unmatched Accounts Per Marketing AO/AB | Insurance Company, Disposition, Items, Amount |
| 4 | Summary Report Per Disposition | Disposition, Insurance Company, Marketing AO/AB, Items, Amount |
| 5 | Production Register | Month of Production, Insurer, Invoice Number, Booking Date, Inception Date, Expiry Date, Policy No., Endorsement No., PN No., Assured Name, Insurance Code, Risk Type, Sum Insured, Basic Premium, Gross Commission, A/R Client, Gross Premium, Booked VAT, Amount Paid, Date Paid, OR Number, Adjustment Type, Remittance Status |

### Reports named in requirement rows

| BRD | Report |
|---|---|
| RMTID.015 | Paid AR higher than DTIP balance |
| RMTID.016 | OR vs Paid PR Exception Report (per upload) |
| RMTID.020 | Accounts excluded (hold / pending negative adjustment) |
| PRCID.019 | List of Unbooked Accounts and Status |
| PRCID.034 | Item count and extraction timestamp |
| PRCID.038 | Unmatched accounts with feedback and disposition |
| ADJID.016 | Adjustment and Daily Endorsement Report |
| ADJID.017 | Validation List |
| ADJID.018 | Validation Slip |
| ADJID.019 | Adjustment Report |
| ADJID.021 | Transaction aging |
| ADJID.026 | Minimal balance write-off summary |
| CMRID.004 | Commission receivable (DP vs regular, net) |
| CMRID.014 | Production per branch and insurer, yearly |
| CSHID.027 | BIR 2307 transaction report |
| BRQID.006 | Batch run report (successful / unsuccessful / applied / unapplied / failed) |

## 8. Non-functional requirements

| Topic | BRD | Approach | Fit |
|---|---|---|---|
| Users / volumes | Cashiering HO 11 + branches 5 (receipts), TL/TH 5+5; Remittance 4; Prod Recon 4; Adjustment 6; Commission receivables 5 + 4 + 2; transaction volumes and growth not given | Size with BRD-1 (145 concurrent) - Operations adds < 50 users | FIT |
| Response time | < 5 seconds for every listed function (BRD-1: 10 s); batch print 50 docs < 10 s; section navigation < 5 s | p95 < 3 s online; heavy work (extraction, matching, batch print) asynchronous with progress | CONFIGURE |
| Peak demand | 15th and 30th of the month (Cashiering, Remittance); 1st-2nd week (Prod Recon); Q4 (Adjustment, Commission); peak hours 08:00-17:30 / 07:30-18:00 | Schedule extraction / matching jobs off-peak (RMTID.001 'off-peak execution') | CONFIGURE |
| Availability | 99.9%; usage 07:00-18:30 (07:30-18:00 Adjustment / Commission); maintenance per bank standard; RTO 4 h, RPO 24 h | HA deployment; the BRD-1 window (07:00-22:00 Mon-Sat) is wider and governs | CONFIGURE |
| Retention | Application, DB, audit logs, historical data: 5 years online, 15 years offline archive, backup every 4 hours, backup retention 7 years (BRD-1: 5 years) | Same retention framework as BRD-1 (BRNB.106) with backup retention 7 years | NEW |
| Anonymisation | No data anonymisation over time | None | FIT |
| Devices | Mobile and desktop users expect the same performance | Responsive UI | FIT |

## 9. Acronyms (Annex I, selection)

| Term | Definition |
|---|---|
| AB | Account Broker |
| ACR | Accounting Controls and Reconciliation |
| ACSL | Accounting Controls and Subsidiary Ledger |
| Adjustment / Negative Endorsement | Negative financial impact to premium and/or commission |
| AO | Account Officer |
| AR - Cashiering | Acknowledgment Receipt |
| AR - ProdRecon | Accounts Receivable |
| BDOI | BDO Insurance Brokers, Inc. |
| Canceled flat | Full cancellation of issued policy, reversal of total premium and taxes |
| Canceled flat retain DST | Full cancellation; premium, taxes and charges reversed except DST, which is for payment by client |
| CLG | Consumer Lending Group |
| CLPC | Consumer Lending Processing Center |
| CoCI | Checks and Other Cash Items |
| DTIP | Due To Insurer Provider |
| For Closure | Insurer confirmed to close discrepant items even if prod recon items are unresolved |
| Internal Adjustment | No endorsement copy or insurer approval required; financial or non-financial |
| IOM | Internal Office Agreement |
| LGT | Local Government Tax |
| PN | Promissory Note |
| Production Register | System-generated summary of booked accounts, extractable for any period by Prod Recon |
| Unbooked accounts | In the insurer's production register but not yet booked by BDOI (direct issuance or timing) |

## 10. Open questions for BDOI

| Q# | Topic | Question | Related |
|---|---|---|---|
| OQ01 | Integration | For each system in BRQID.004 (Collection, Accounting, Disbursement, Marketing, Claims): is it an existing BDO/BDOI system or a BrokerVerse module of a later BRD? Direction, data, interface (API / file / DB), frequency, and who owns the specification. | BRQID.004-005, CSHID.009/026, RMTID.030, CMRID.001/009 |
| OQ02 | Disbursement | What is 'Disbursement' (system or unit)? Payload of 'push to Disbursement' (remittance batch, payment request, 2307 report), and how DV numbers / payment status return (needed for Fully / Partially Remitted). May the BrokerVerse payables module (payment vouchers) act as Disbursement? | RMTID.002/011/019/034, CSHID.026/027, DBMID.001 |
| OQ03 | Payment files | Sample files and record layouts (encoding, header / trailer, control totals) for Bills Payment FS01 (IT-DCO), Trade (CIB), CLPC Excel, PDC Excel ('PDC from PMS' - what is PMS?), Direct Credit FS01/04. Who uploads: Operations users, or IT batch (FS04) as stated for Trade / Direct Credit / CLPC? | CSHID.008 |
| OQ04 | File integrity | 'Files are encrypted and cannot be edited': are source files PGP-encrypted by the sender, or is the requirement that BrokerVerse stores them read-only with a checksum? | CSHID.008 |
| OQ05 | Receipt series | AR / OR numbering: BIR ATP series per branch? Format and length; pre-printed vs system-generated receipts (CAS registration); low-series warning threshold; are AR numbers BIR-controlled like ORs? | CSHID.001/002/006/015/019 |
| OQ06 | Cancel / reinstate | Do AR / OR cancellation and reinstatement need checker approval (TL / TH)? Definition of full vs partial reinstatement; accounting per reason. | CSHID.001-005 |
| OQ07 | Chart of accounts and entries | Comptrollership default entries and GL accounts for: AR premium (trust cash), payment application by PR component, unapplied / AP unapplied payments, AP overages, PR2307, DTIP, commission (realized on collection?), VAT on commission, WTAX, AR Insurance / AR Insurer, incentives, write-off, cancellation (flat / retain DST / partial), DP PR reversal. | CSHID.012-016, ADJID.011-013, MKTID.012 |
| OQ08 | Exchange rate | 'Comptrollership exchange rate table' with 2 decimals: which rate (buying / selling / booking), daily or monthly, and which date (receipt vs booking)? Treatment of FX difference between booking and collection of dollar accounts. | CSHID.012-014 |
| OQ09 | Application hierarchy | Component hierarchy DST -> VAT -> LGT -> Other -> Basic confirmed; meaning of mode-of-payment hierarchy 'Check, Cash'; order across several invoices of one payer (oldest first?); hierarchy for commission receipts. | CSHID.022, CSHID.020 |
| OQ10 | 2% CWT | Which clients / invoices are 2% CWT (flag source)? Apply 98% and keep 2% as PR2307 until certificate - confirm. CSHID.026 cash path says 'premium is reversed, remittance processed': which entries? | CSHID.020/026/016, MKTID.010 |
| OQ11 | Minimal balances | Reconcile the three rules: CSHID.016 (PR <= 10.00 reversed unless equal to 2% CWT / DST / entire premium, compared to PR2307), summary 5.f (<= 10.00 to AP overages) and ADJID.026 (10.00-100.00 write-off or credit). Boundary at exactly 10.00, GL targets, content of the minimal balance table, approval. | CSHID.016, ADJID.026 |
| OQ12 | Pre-booked matching | Key used to match a payment to a pre-booked account (ARN, invoice no., PN no.?), re-run schedule, retention in the pre-booked table. BRD-1 placement also matches CLPC payments before placement (BRNB.067/068): confirm Cashiering is the single payment intake and the placement gate consumes its result. | CSHID.020, CSHID.008 |
| OQ13 | Check pick-up | How does the Collection 'for check pick' status reach Operations? Is the AR printed and handed over at pick-up? | CSHID.009 |
| OQ14 | AR Insurance | Definition and identification of non-premium insurer payments (refunds of remitted premium, others) and the AR Insurance accounting. | CSHID.021, ADJID.012 |
| OQ15 | Dispositions | Full disposition type list, which require approval and by whom, and the refund hand-off to Collection / Disbursement. | CSHID.024/025, ADJID.013 |
| OQ16 | BIR 2307 flow | Certificate statuses, sorting per insurer, when Dr DTIP / Cr PR2307 is posted (on validation or on release to insurer), and whether Marketing tagging stays in BrokerVerse or comes from the Collection system. | CSHID.026/027, MKTID.010/013, DBMID.001 |
| OQ17 | Remittance types / schedule | How an invoice is classified With Incentives / Normal-Dollar / Normal-Peso; extraction schedule ('Batch Run: TDB'); file naming; shared-drive path, or is an in-system repository acceptable? | RMTID.001/003/007 |
| OQ18 | EOD trigger | Meaning of 'trigger extraction at end of day if payment application is searched' (RMTID.005, summary 1.e). | RMTID.005 |
| OQ19 | Paid AR > DTIP | Expected result says remit only the DTIP balance; acceptance criteria say flag and exclude. Which one? | RMTID.014/015 |
| OQ20 | Check clearing | Holding period: 3 banking days from AR date or deposit date? Source of 'cleared' status (bank reconciliation, bank file, manual)? | RMTID.017/018/028 |
| OQ21 | Remittance approval | Who approves a remittance batch (TL / TH) and the full status model; RMTID.019 mixes payment statuses (Unapplied, Outstanding Balance) with remittance statuses. | RMTID.010/019/029 |
| OQ22 | Insurer OR upload | Layout of the insurer-populated remittance schedule; OR amount vs paid PR comparison level (invoice / batch) and tolerance; why is MKTID.001 (send schedule to insurer) a Marketing request? | RMTID.012/013/016, MKTID.001 |
| OQ23 | Early remittance incentive | Rates per insurer / product (2% for CLG / CBG motor and fire?), window start (inception vs booking), deduction from the remittance vs separate OR, and the difference between RMTID.023 and PRCID.028. | RMTID.023, PRCID.028 |
| OQ24 | Hold remittance | Who requests and approves holds (Marketing AO / TL / UH), maximum hold and extension rules, whether hold data will flow in from a Marketing system. | MKTID.002-006, RMTID.021 |
| OQ25 | Special remittance | Approvers and SLA; exact eligibility for claims / renewal / installment / immediate OR. | MKTID.009, RMTID.030 |
| OQ26 | Write-off | Who writes off accounts and through which transaction (adjustment request type 'Write-off', minimal balance file)? | RMTID.022, ADJID.026 |
| OQ27 | Estimated items | Definition of 'estimated items' in production reports. | RMTID.037, CMRID.014 |
| OQ28 | Invoice locking | Which Comptrollership activity holds an invoice ('still with comptrollership')? List of lock reasons. | RMTID.040 |
| OQ29 | Prod recon files | Extraction frequency per insurer; production register template and naming (TBD); cover letter; password convention; channel to send and receive insurer reports (e-mail, SFTP, portal). | PRCID.001-009 |
| OQ30 | Recon matching | Keys (PRCID.022: policy + invoice; PRCID.027: eight fields); is the 1.00 tolerance per amount field or total; definition of 'matched with discrepancy'; automatch frequency (left blank). | PRCID.022-027 |
| OQ31 | Recon lists | Company-concerned list (to be provided), disposition list for unmatched items, 'For Closure' handling. | PRCID.016/033/038/039 |
| OQ32 | Endorsement ownership | Split between BRD-1 booking / adjustment (BRNB.076/081/094) and this BRD (ADJID): who requests (Marketing) vs processes (Adjustment); endorsement request number format; endorsement reference (insurer) vs request no.; endorsement document list (ADJID.025 blank). | ADJID.001/003/015/020/023/025, MKTID.008 |
| OQ33 | Package limits / co-insurance | Package TSI limits and insurer capacity per product; co-insurance: one invoice with shares (ADJID.027) - lead insurer and remittance per insurer? | ADJID.008/027 |
| OQ34 | Service invoice on adjustments | Addendum ADJID.014: for a decrease in commission / income, is a credit memo or a negative service invoice issued? Numbering and recipient. | ADJID.014, BRNB.100 |
| OQ35 | Endorsement type names | 'Extension of cover' is both financial (ADJID.002e / 010, additional premium, approval) and non-financial (ADJID.004 items 2-4). Confirm the list and who approves additional premium. | ADJID.002/004/010 |
| OQ36 | Refund computation | Partial cancellation and decrease of TSI: pro-rata or short-period rate; 'Canceled flat retain DST'; commission recovery. | ADJID.009/014 |
| OQ37 | Over-adjustment | Baseline / limit values for allowable adjustments. | ADJID.028 |
| OQ38 | Direct payment | DP list sources (shared folders vs Collection system), naming and schedule; how 'fully paid to insurer' is confirmed; DP PR reversal entries; commission billing format; insurer response file. | CMRID.001-013, MKTID.011/012 |
| OQ39 | Incentive schemes | No Touch / Top Up targets, tiers, rates, multipliers and periods; Motor Mania amounts, thresholds and period; who pays whom; is 'pass-on to branches' a payout by BDOI (via Disbursement)? Relation to the BRD-1 incentive flag (BRNB.107, Q33). | CMRID.003/005/006 |
| OQ40 | Insurer feedback | Feedback reason list; start of the 10-working-day SLA. | CMRID.008/009/011 |
| OQ41 | BIR certificates | CMRID.015 certificates are insurers' EWT certificates on commissions paid to BDOI: confirm, and the Comptrollership acknowledgment roles. | CMRID.010/015 |
| OQ42 | Report layouts | Fields / formats for reports without detail in the Annex (Cashiering 9-18, 20-21; Remittance 8) and for CMRID.004 / 014. | CSHID.023, RMTID.039, CMRID.004/014 |
| OQ43 | Ageing | Ageing buckets for Cashiering reports ('with aging'). | CSHID.023 |
| OQ44 | NFR alignment | Operations availability 07:00-18:30 vs BRD-1 07:00-22:00; response < 5 s vs 10 s; backup retention 7 vs 5 years; RPO 24 h vs 4-hour backups. | NFR |
| OQ45 | Marketing scope | Are the MKTID activities (hold, special remittance, 2307 tagging, DP tagging, send schedule) delivered in the Operations build or by a Marketing / Collection BRD that will feed Operations? | MKTID.001-013 |
| OQ46 | Claims | What does Operations need from the Claims system (e.g. special remittance for claims)? | BRQID.004, MKTID.009 |
| OQ47 | Search logs | Purpose and retention of search logs (CSHID.010 AC 7). | CSHID.010 |
| OQ48 | Roles | Operations user access matrix (personas: Cashier HO / branch, TL, TH, Remittance Processor, Prod Recon Handler, Adjustment Processor, Commission Handler, Marketing Collection, Comptrollership, Disbursement) - 'user roles and authorization will be further defined' (Addendum). | BRQID.001-003, RMTID.002 |
| OQ49 | Commission OR grouping | CSHID.007 says 'one OR per remittance batch number' and 'OR per individual payment unless consolidation criteria are met': which prevails? | CSHID.007 |
| OQ50 | Co-insured remittance | For co-insured invoices, is remittance extracted and scheduled per insurer share? | ADJID.027, RMTID.001 |

## 11. Observations on the BRD pack

- The addendum (pp.1-7) supersedes **RMTID.002**. The main BRD allowed editing of the extracted remittance file and pushing it "with or without edit". The addendum allows only row exclusion and forbids changes to financial fields. The addendum also extends **ADJID.014** with service-invoice generation. Both IDs appear twice in the PDF and are recorded once, on the updated row.
- **ADJID.015** (persona "User", with the note "This is a Marketing activity") and **MKTID.008** describe the same Endorsement Slip. They are built once and exposed to both personas.
- **RMTID.023** and **PRCID.028** both automate the early-remittance incentive (30-day window). PRCID.028 limits it to CLG / CBG motor and fire at 2%. See OQ23.
- There are three minimal-balance rules that do not line up. CSHID.016 covers PR balances of 10.00 or less and compares them with the PR2307 balance, while its criteria list 2% CWT, DST and the entire premium. Summary 5.f sends balances of 10.00 or less to AP overages. ADJID.026 covers 10.00 to 100.00. The value 10.00 falls in both ranges. See OQ11.
- **RMTID.014** conflicts with itself. The expected result caps the remittance at the DTIP balance, while the acceptance criteria exclude the account. See OQ19.
- RMTID.019 mixes payment states (Unapplied Payment, With Outstanding Balance) with remittance states. See OQ21.
- **CSHID.007** says both "one OR per remittance batch number" and "OR per individual payment unless consolidation criteria are met". See OQ49.
- "Extension of Cover" is a financial type in ADJID.002e and ADJID.010, where it carries additional premium and needs approval. It is also a non-financial type in ADJID.004 (clauses with no premium, and moving the period without financial impact). See OQ35.
- Several values are still open in the BRD: RMTID.001 "Batch Run: TDB"; PRCID.004 naming convention "TBD"; the PRCID.024 / 025 automatch frequency is blank; the PRCID.016 company list is "to be provided"; the ADJID.025 document list is blank; the fields of Cashiering reports 9-18 and 20-21 and Remittance report 8 are blank.
- CSHID.008 says the Trade, Direct Credit and CLPC files are "manually prepared by BU, send to IT, IT to run batch process - FS04". This conflicts with Operations users uploading the files. The PDC list is described both as an Excel upload and as "PDC from PMS". See OQ03.
- Several IDs have no key-capability number: CSHID.026 / 027, RMTID.038 / 040, ADJID.026-028, CMRID.015 and MKTID.010-013. MKTID numbering does not follow the document order: MKTID.013 is in Cashiering and MKTID.008 comes last, in Adjustment.
- The Marketing (MKTID) and Disbursement (DBMID) items are requirements on other units. The BRD also says Operations will be "further integrated to the other systems". It is not clear whether they are built in Operations or received from the Collection / Disbursement systems. See OQ45 and OQ02.
- The acronym table gives DTIP as "Due To Insurer Provider", and BDOI as "BDO Insurance Brokers, Inc.". Elsewhere the company is BDO Insurance and Reinsurance Brokers.
- The volume table of Commission Receivables is headed "Adjustment / Endorsement", which looks like a copy error. Transaction volumes and growth rates are blank for every area.
- The Operations NFRs differ from BRD-1: response time < 5 s instead of 10 s, service hours 07:00-18:30 instead of 07:00-22:00 Mon-Sat, and backup retention 7 years instead of 5. See OQ44.
- In the addendum approval page a reviewer date is written "1/6/2025", probably 2026. The main BRD approval sheet (pp.121-123) is signed; one input provider is marked "on maternity leave".
- Several requirements overlap with BRD-1. MKTID.011 (DP tagging at quotation / policy) is BRNB.114. ADJID.001 / 003 / 011 overlap BRNB.076 / 081 / 094 (endorsements and cancellation in booking). The CLPC payment upload (CSHID.008) overlaps the CLPC payment matching of BRNB.067 / 068 (placement). See OQ12 and OQ32.
- CSHID.026 ("premium is reversed, remittance is processed" for 2307 cash payments) gives no accounting entries. CSHID.027 / DBMID.001 give Dr PR2307 / Cr PR and Dr DTIP / Cr PR2307. See OQ16.
