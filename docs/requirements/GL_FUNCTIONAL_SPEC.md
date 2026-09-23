# iNXT FinVerse: Insurance GL Functional Specification (condensed)

Sources:
- `Premia General Ledger Module.txt` (292-page functional/solution design; referred to as **PGL**)
- `GL Module.txt` + `GL Module FAQ.txt` (Polaris/bank-style GL feature list; the FAQ repeats GL Module.txt; referred to as **GLM**)
- `GL Module Additional Screens.txt` (screen titles only; referred to as **GLS**)

Tagging convention:
- Plain bullets are taken from the docs. Lists marked "(verbatim)" copy the doc's wording.
- **[IMPL]** marks an implementation decision or gap-fill that FinVerse needs but the docs do not specify. Treat it as a proposal and confirm it with the business.
- Section numbers in parentheses, such as (PGL 3.1.6), point back to the source.

Gaps to know up front:
- PGL sections 2.4 (Accounting Event Configuration) and 2.5 (Accounting Rules Configuration) contain only topic headings; their body text is missing.
- PGL gives no numeric event codes, no account numbers and only five Dr/Cr mapping pairs (2.3). Everything else in the §4.6 Dr/Cr table is [IMPL].
- PGL has no "soft close" status; §8 maps it onto the doc's `Closing` status.
- PGL has no "Division" entity. It lists Division only as an organizational dimension (1.13.6).
- Several Part B sections (1.5.9+ series summaries, 1.7.9–1.7.15, 1.8.9–1.8.16, 1.9.9–1.9.16, 1.10.9+, 1.11.9+, 1.12.9+, 1.13.9+, 1.6.13+) are announced but never written.

---

## 0. Architecture principles (drive every module)

- Accounting is event-driven. The pipeline is: Insurance Transaction → Business Event → Accounting Event → Accounting Rule Engine → Journal Generation → Posting Engine → GL → Trial Balance → Financial Statements → Regulatory Reports (PGL 1.1.9).
- Accounting logic is configuration-driven, not hard-coded. "Configuration Over Customization". Rules are version-controlled and effective-dated (PGL 1.2.4, 1.7.6).
- Operational modules never write GL entries directly. They emit standardized accounting events (PGL 1.2.2).
- "Capture Once – Process Everywhere": one event is reused for accounting, reporting, audit and reconciliation (PGL 1.5.9).
- No accounting event is generated until the originating business transaction has completed successfully (PGL 1.5.4).
- The GL stores summarized postings that keep references back to the source (policy no, claim no, RI ref, source module, document) (PGL 1.2.5, 1.10.4).
- Real-time/OLTP posting: authorized transactions update balances immediately, with no dependency on end-of-day processing (PGL 1.2.6; GLM 10, TP 3).
- Posted transaction records are **immutable**. Corrections are made only by reversal or adjustment journals (PGL 1.11.8, 3.1.10).
- Each posting is atomic (all-or-nothing), with rollback, duplicate detection and event replay (PGL 1.5.18.3, 1.9.6).
- Architectural principles (verbatim, PGL 1.5.23): Event-Driven Processing; Loose Coupling Between Services; Separation of Business and Financial Logic; Configuration Over Customization; Stateless Service Design; Centralized Accounting Rules; Single Source of Financial Truth; End-to-End Traceability; High Availability by Design; Security by Design; Auditability by Design; Scalability by Design.
- Layers (PGL 1.2.8): Presentation → Business → Accounting Rule → Journal Processing → Posting → Reporting.
- Core components (PGL 1.3.5, 1.8): GL Master, Accounting Event Manager, Accounting Rule Repository/Engine, Voucher Generation Engine, Journal Processing Engine, Posting Engine, Currency Management Engine, Tax Engine, Financial Period Manager, Audit Manager, Reporting Engine.
- Services, which map to API endpoints (PGL 1.3.6, verbatim): Create Journal; Validate Journal; Post Journal; Reverse Journal; Validate Financial Period; Validate Currency; Retrieve Account Balance; Generate Trial Balance; Generate Financial Statement; Generate Audit Report.
- Event-engine services (PGL 1.5.11): Event Registration, Validation, Accounting Rule, Journal, Posting, Audit, Notification.
- Environments (PGL 1.5.20.3): Development, SIT, UAT, Pre-Production, Production.
- Integrated modules (PGL 1.1.6, 4.6.4): Customer Mgmt, Product Config, Underwriting, Quotation, Policy Admin, Endorsements, Renewals, Premium Billing, Premium Receipts/Collections, Claims, Reinsurance, Coinsurance, Agency Mgmt, Broker Mgmt, Commission, Investments, Treasury, AP, AR, Fixed Assets, Banking Interfaces, Payments, External ERP, Regulatory Reporting, Enterprise Data Warehouse.
- Functional scope (PGL 1.1.5, verbatim): General Accounting; Premium Accounting; Claims Accounting; Commission Accounting; Reinsurance Accounting; Coinsurance Accounting; Investment Accounting; Bank Accounting; Cash Management; Inter-Branch Accounting; Inter-Company Accounting; Tax Accounting; Foreign Currency Accounting; Financial Reporting; Regulatory Reporting.
- Accounting layers (PGL 1.4): Operational → Financial → Technical → Management → Regulatory accounting.

### 0.1 Master vs transaction data (PGL 1.3.3.7, 1.11.4)

- **Master data:** Company Master; Branch Master; Chart of Accounts; Account Groups; Currency Master; Exchange Rate Master; Financial Year; Accounting Period; Voucher Types; Accounting Events; Accounting Rules (Posting Rule Master); Cost Centre; Profit Centre; Financial Dimension Master.
- **Transaction data:** Journal Header; Journal Detail; GL Transactions; Posting Transactions; Account Balances; Period Balances; Currency Balances; Balance Movements; Posting History; Reversal History/Transactions; Audit Trail.
- **Reference data:** Policy, Claim, Reinsurance, Commission, Payment, Business Event and Source Module references.
- **Historical data:** Archived Journals; Archived Ledger Transactions; Historical Balances; Closed Financial Periods; Audit History.
- **Master data rules:** unique business identifiers; centralized maintenance; version control; effective-date management; status management; validation rules; referential integrity (PGL 1.11.7).
- **Entity relationship chain:** Company → Branch → Chart of Accounts → Journal Header → Journal Detail → GL Transaction → GL Balance → Financial Reports (PGL 1.11.6).
- **Repositories:** Master Data; Transaction; Balance (Account/Period/Currency/Cost Centre/Profit Centre balances); Audit (immutable) (PGL 1.5.15).
- **[IMPL]** Every master uses a common governance set: `status`, `effective_from`, `effective_to`, `version`, `created_by/at`, `modified_by/at`, `authorized_by/at`. Masters have **no hard delete**; use deactivation instead (GLM 16).

---

## 1. Enterprise Organization Structure (PGL 2.1; GLM 1, 4, 6)

### 1.1 Hierarchy

- Organizational hierarchy: Enterprise → Company → Region → Branch → Department → Business Unit → Operational User (PGL 2.1.2).
- Configuration order: Enterprise → Company → Region → Branch → Department → Business Unit → User Assignment → Financial Activation (PGL 2.1.9).
- Each level inherits controls from its parent but operates independently.
- Division: the doc lists it only as an organizational dimension ("Company, Branch, Business Unit, Department, Division"; PGL 1.13.6). **[IMPL]** Model Division as an optional level between Company and Branch (or as a dimension), selected by configuration.

### 1.2 Entities and key fields

- **Enterprise:** enterprise financial policies, accounting standards, corporate reporting, enterprise security, consolidated statements, regulatory compliance.
- **Company** (a legal and accounting entity with its own books): Company Code; Company Name; Base Currency; Financial Calendar; Chart of Accounts; Accounting Policies; Tax Configuration; Regulatory Information; Operational Status.
- **Region:** code, name, parent company. (The doc gives no fields.) **[IMPL]**
- **Branch/Office:** Branch Code; Branch Name; Parent Company; Regional Association; Financial Responsibility; Operational Status; Branch Manager; Reporting Hierarchy.
  - Also from GLM Office Master: address; date of opening; forex authorization flag; MICR code; holiday schedule; weekly holidays; contact info.
- **Department** (typical, verbatim): Underwriting; Claims; Reinsurance; Finance; Investments; Customer Service; Information Technology; Human Resources.
- **Business Unit** (typical, verbatim): Life Insurance; Health Insurance; Motor Insurance; Property Insurance; Marine Insurance; Travel Insurance; Corporate Insurance.
- **Holiday/Calendar Master** (GLM 4): calendars can be global, branch-wise, currency-wise or state-wise, and define working days and holidays.

- **1.3 Lifecycle:** Entity lifecycle (verbatim): Draft → Verification → Approval → Activation → Operational Use → Modification → Deactivation → Archival (PGL 2.1.11).
  - Every transition goes through an authorization workflow and is audited.
- **1.4 Controls:** Company Validation; Branch Validation; Active Status Verification; Financial Calendar Validation; Base Currency Validation; User Authorization; Inter-Company Transaction Controls; Organizational Hierarchy Validation (PGL 2.1.10).
  - Security scoping: Company-Level, Branch-Level, Department-Level and Business Unit Access; Role-Based Security; Maker-Checker; SoD; Administrative Privileges (PGL 2.1.12).
  - Postings are not allowed against inactive companies or branches. These fail with posting exceptions "Inactive Company" or "Inactive Branch" (PGL 3.2.9).

### 1.5 Screens and reports

- Screens: Enterprise setup; Company master; Region master; Branch/Office master; Department master; Business Unit master; Holiday/Calendar master; User-org assignment; Org hierarchy tree viewer.
- Reports (verbatim): Enterprise Financial Reports; Company Financial Statements; Regional Performance Reports; Branch Profitability Reports; Departmental Expense Analysis; Business Unit Performance Reports; Consolidated Financial Reports.

---

## 2. Chart of Accounts (PGL 1.10.6, 1.12, 2.2, 2.3; GLM 2, 3, 5, 6, 13–16, 18, 28–30, 43–45; GLS)

### 2.1 Levels and hierarchy

- PGL 1.12.5: Enterprise → Company → Account Group → Major Account → Control Account → Sub Account → Transaction Account.
- PGL 1.10.6 gives the same hierarchy without the Enterprise level: Company → Account Group → Major Account → Control Account → Sub Account → Transaction Account.
- PGL 2.3 gives an alternative view: Company → Branch → Business Unit → Account Group → Ledger Account → Sub-Ledger (Optional).
- GLM tiers: **Main GL (GL Head) → Subsidiary GL (Sub GL) → Micro GL**, plus optional **Contra GLs**. There is no limit on Sub GLs per Main GL or Micro GLs per Sub GL. Updates cascade automatically from lower tiers to higher tiers (GLM 3, 29, 43).
- The CoA is defined globally and linked to each branch by Branch Code. Branch-level balances are kept separately, with central consolidation (GLM 6).
- Hierarchy capabilities: parent-child relationships, roll-up, configurable account groups, activation and deactivation, reporting classifications, multi-company account mapping.
- **[IMPL]** Model the CoA as a tree with a `level` enum (GROUP, MAJOR, CONTROL, SUB, TRANSACTION). Only TRANSACTION (leaf) accounts can be posted to. The docs require "direct posting to GL Heads of Accounts" to be prevented (GLM 43).

- **2.2 Account code format (PGL 1.12.7):** Format: `CC-AAAA-BBBB-CCCC`
    - CC = Company Identifier
    - AAAA = Account Group
    - BBBB = Major Account
    - CCCC = Transaction-Level Account
  - Segments: Company Code, Account Category, Major Account, Sub Account, Transaction Account.

### 2.3 Account classes and types

- Primary classifications (PGL 1.12.4): Assets; Liabilities; Equity; Revenue; Expenses; Memorandum Accounts.
- PGL 2.2 adds functional categories: Bank Accounts; Cash Accounts; Tax Accounts; Control Accounts. PGL 2.3 uses "Income" as a synonym for Revenue.
- Examples (verbatim, merged from PGL 1.12.6 and 2.3):
  - **Asset:** Cash; Bank Accounts; Investments; Premium Receivable; Reinsurance Recoverable; Fixed Assets.
  - **Liability:** Claims Outstanding; Claims Payable; Unearned Premium Reserve; Commission Payable; Reinsurance Payable; Tax Payable; GST Payable; Outstanding Expenses.
  - **Equity:** Share Capital; Retained Earnings; Current Year Profit; Capital Reserve; Reserves & Surplus.
  - **Revenue/Income:** Premium Income; Reinsurance Commission (Income); Investment Income; Service Charges; Other Operating Income.
  - **Expense:** Claims Paid/Claims Expense; Commission Expense; Administrative Expense; Employee Expenses; Reinsurance Cost; Depreciation.
- Technical-reserve accounts required (PGL 1.4.5): Unearned Premium Reserve (UPR); Deferred Acquisition Cost (DAC); Outstanding Claims Reserve (OCR); Incurred But Not Reported (IBNR); Incurred But Not Enough Reported (IBNER); Premium Deficiency Reserve (PDR); Catastrophe Reserves; Equalization Reserves (where applicable).
- Special GL types from GLM:
  - **Nominal GL:** carries a preceding entry type and a reversal type. Originating entries get unique nominal numbers so responding entries can be matched. Supports auto-reconciliation and outstanding-by-age reports (GLM 18, 28, 44).
  - **Contra GL** (GLM 3).
  - **Main Cash GL** with subsidiary GLs for each cash account (GLM 12, 22).
  - **Suspense accounts:** reviewed at period-end (PGL 3.3.8).
- **[IMPL]** Add `normal_balance` (DR/CR), derived from class. Add a `nature` flag: Permanent (balance sheet, carried forward) vs Temporary (P&L, closed at year-end) (PGL 3.10.6).

### 2.4 Account master attributes

- PGL 1.12.4: Account Code; Account Name; Account Type; Parent Account; Account Category; Currency; Status; Effective Date; Reporting Classification.
- PGL 2.3: Branch Mapping; Cost Centre; Status (Active/Inactive); Financial Reporting Group.
- GLM access and usage controls:
  - allowed currencies ("Currencies Allowed for a GL", GLS)
  - posting controls (branch-wise and currency-wise)
  - user-wise access ("Access Codes for GL Heads", GLS)
  - posting branch codes and eligibility criteria
  - Profit/Cost Centre codes required at posting
  - account freezing
  - GL Category ("General Ledger Category Master", GLS)
  - GL closure ("GL Closure" screen, GLS)
- **[IMPL]** Additional attributes:
  - `is_control_account`
  - `subledger_type` (Customer / Broker / Agent / Reinsurer / Vendor / Bank / Policy / Claim / None)
  - `allow_manual_posting`
  - `mandatory_dimensions[]`
  - `revaluation_flag`
  - `reconciliation_flag`
  - `intercompany_flag`
  - `group_account_code` (consolidation mapping)
  - `is_frozen`

### 2.5 Control accounts and sub-ledgers

- Control accounts sit in the hierarchy (Control Account level). The posting engine performs "Control Account Update" and "Sub-Ledger Update" (PGL 1.3.3.6).
- The AR sub-ledger manages Outstanding Premiums; Customer, Broker and Agent Receivables; Recovery Tracking (PGL 2.2).
- The AP sub-ledger manages Claims Payable; Commission Payable; Reinsurance Payable; Vendor Payments; Tax Payables (PGL 2.2).
- **[IMPL]** Control accounts are posted only by system events that carry a sub-ledger party. Manual journals to them are blocked unless a party is supplied. The balance of each control account must equal the sum of its sub-ledger balances, checked in the sub-ledger reconciliation (§12).

- **2.6 Validation rules (PGL 1.12.4, 2.3):** Valid GL Account; Active Account Status; Effective Date; Company Eligibility; Branch Eligibility/Mapping; Currency Compatibility; Posting Permission; Financial Period Status; Debit equals Credit; Authorization Rules.
- **2.7 Governance:** Activities: Account Creation; Modification; Activation; Deactivation; Hierarchy Maintenance; Classification Management; Approval Workflow; Version Control; Change History (PGL 1.12.8).
  - No delete option. Modifications need higher-level authorization (GLM 16).
  - COA maintenance functions: Create Ledger Accounts; Modify; Activate/Deactivate; Group Accounts; Map Accounts to Insurance Transactions; Year-End Balance Carry Forward (PGL 2.3).

### 2.8 Screens

- GLS screen list (verbatim): General Ledger Heads Maintenance; Sub GL Heads Maintenance; Micro GL Heads Maintenance; General Ledger Category Master; Currencies Allowed for a GL Maintenance; Access Codes for GL Heads Maintenance; GL Posting Control Maintenance; GL Posting Controls for Other Branch GL; GL Closure; Currency Rate Type Master Maintenance.
- **[IMPL]** Also: CoA tree explorer; account detail with change history; account-to-event mapping view; group-CoA mapping.

---

## 3. Financial Dimensions (PGL 1.10.7, 1.13, 1.4.6)

- **3.1 Dimension lists:** Dimensions (PGL 1.10.7): Company; Branch; Department; Cost Centre; Profit Centre; Product; Line of Business; Channel; Region; Currency.
  - Management-accounting dimensions (PGL 1.4.6): Company; Branch; Region; Product; Line of Business; Distribution Channel; Agent; Broker; Cost Centre; Profit Centre; Department; Business Unit.
  - Dimension hierarchy (PGL 1.13.5): Enterprise → Company → Business Unit → Branch → Department → Cost Centre → Profit Centre → Product → Channel → Region → Project.

### 3.2 Classification (PGL 1.13.6, verbatim)

- **Organizational:** Company, Branch, Business Unit, Department, Division.
- **Operational:** Product, Policy Type, Line of Business, Distribution Channel, Underwriting Office.
- **Financial:** Cost Centre, Profit Centre, Budget Centre, Investment Portfolio.
- **Geographical:** Country, Region, State, Zone, Territory.

- **3.3 Dimension master:** Fields: Dimension Code; Dimension Name; Dimension Type; Parent Dimension; Status; Effective Date; Reporting Hierarchy.
- **3.4 Assignment and resolution:** Sources: Company, Branch and Product config; Policy info; Claim info; Accounting Rules; Organizational Hierarchies; Default Dimension Rules.
  - The resolution engine performs: identification; rule evaluation; hierarchy resolution; validation; default assignment; inheritance processing.
- **3.5 Validation:** Active Dimension; Organizational Eligibility; Effective Date; Hierarchy; Duplicate Prevention; Mandatory Dimension Verification.
  - If a required dimension is missing, posting fails with exception "Missing Financial Dimensions" (PGL 3.2.9).
- **3.6 Data model:** Journal lines carry: Cost Centre, Profit Centre, Department, Product, Business Unit (PGL 3.1.3).
  - **[IMPL]** Store dimensions as FK columns for the core set (company, branch, department, cost_centre, profit_centre, product, lob, channel, region, business_unit) plus a JSON/EAV extension for configurable dimensions.
  - Balances are kept per dimension combination: Cost Centre and Profit Centre balances (PGL 1.9.4).

### 3.7 Screens and reports

- Screens: Dimension type master; dimension value master (tree); dimension default rules; mandatory-dimension-by-account config.
- Reports: Dimension-based aggregation; Cost Analysis; Profitability Analysis; Departmental, Regional and Product reporting; Cost Centre Reports; Profit Centre Reports.

---

## 4. Accounting Events (PGL 1.3.3.3, 1.4.8, 1.5, 1.6.6, 2.2, 2.4)

### 4.1 Event codes named in the doc (verbatim, PGL 1.3.3.3 / 1.6.6)

`POLICY_ISSUE`, `POLICY_RENEWAL`, `POLICY_CANCELLATION`, `PREMIUM_RECEIPT`, `PREMIUM_REFUND`, `ENDORSEMENT`, `CLAIM_REGISTER`, `CLAIM_PAYMENT`, `COMMISSION_PAYABLE`, `REINSURANCE_PREMIUM`, `REINSURANCE_RECOVERY`, `BANK_RECEIPT`, `BANK_PAYMENT`

### 4.2 Event categories and events

Accounting event classification (PGL 1.4.8, verbatim):

- **Premium Events:** Premium Raised; Premium Received; Premium Refund; Premium Adjustment
- **Policy Events:** Policy Issue; Policy Renewal; Policy Cancellation; Policy Endorsement
- **Claims Events:** Claim Registration; Claim Reserve; Claim Settlement; Claim Recovery
- **Commission Events:** Agent Commission; Broker Commission; Override Commission
- **Reinsurance Events:** Treaty Premium; Facultative Premium; RI Recovery; RI Commission
- **Banking Events:** Cash Receipt; Bank Receipt; Bank Payment; Bank Reconciliation

Business events add further items (PGL 1.5.3):

- **Policy:** Proposal Approved; Policy Issued; Policy Renewed; Policy Cancelled; Policy Reinstated; Endorsement Processed
- **Premium:** Premium Calculated; Premium Invoiced; Premium Collected; Premium Refunded; Installment Generated; Installment Paid
- **Claims:** Claim Registered; Claim Reserved; Claim Approved; Claim Paid; Claim Recovery; Salvage Recovery
- **Commission:** Agent Commission; Broker Commission; Override Commission; Commission Recovery
- **Reinsurance:** Treaty Premium; Facultative Premium; RI Commission; RI Recovery; RI Settlement

Other sources:

- Event categories for configuration (PGL 2.4.4, verbatim): Policy Events; Endorsement Events; Premium Events; Claims Events; Reinsurance Events; Commission Events; Payment Events; Receipt Events.
- Automated-accounting list (PGL 2.2): Policy Issuance; Premium Collection; Premium Refund; Policy Cancellation; Endorsements; Claims Payment; Claims Reserve; Reinsurance Premium; Reinsurance Recovery; Commission Payment; GST/Tax; Bank Transactions.
- Technical events (PGL 1.4.5): UPR, DAC, OCR, IBNR, IBNER, PDR, Catastrophe and Equalization reserves. These come from actuarial inputs.
- Coinsurance appears as an integrated module but no events are listed for it. **[IMPL]** Add `COINS_PREMIUM_SHARE` and `COINS_CLAIM_SHARE`.

- **4.3 Event master and classification:** Classification axes (PGL 1.5.6):
    - **Business Function:** Underwriting, Policy Administration, Claims, Reinsurance, Commission, Finance
    - **Financial Nature:** Income, Expense, Asset, Liability, Equity
    - **Transaction Type:** Creation, Modification, Cancellation, Adjustment, Reversal, Settlement
    - **Processing Priority:** Real-Time, Batch, Scheduled, Year-End
  - **[IMPL]** Event master fields: event_code; name; category; business_function; financial_nature; transaction_type; processing_priority; source_module; voucher/journal type; requires_approval; active; effective dates; version.

### 4.4 Event payload (transaction context)

- Event repository fields (PGL 1.5.5): Event ID; Event Code; Source Module; Source Document Number; Company; Branch; Product; Line of Business; Customer; Policy Number; Claim Number; Currency; Transaction Amount; Transaction Date; Event Status.
- Added by PGL 1.6.7: Broker; Agent; Exchange Rate; Financial Period; Effective Date; Posting Date.
- Added by PGL 1.3.3.3: Accounting Date.
- **[IMPL]** Add `idempotency_key` = source_module + source_doc_no + event_code + sequence, used for duplicate detection. Add `amount_components[]` (e.g., net premium, tax, stamp duty, commission) so one event can drive multi-line journals.

### 4.5 Event lifecycle and processing

- Pipeline (PGL 1.5.12): Business Transaction → Business Event Generation → Event Registration → Event Validation → Event Classification → Accounting Rule Evaluation → Journal Generation → Journal Validation → Authorization → Posting → Balance Update → Audit Recording → Reporting.
- The Event Manager: receives events; validates completeness; assigns a unique Event ID; timestamps; identifies source module; classifies; forwards.
- Event queue: sequential per partition; prioritization; failure isolation; recovery. Partition keys are Company, Branch, Product, LoB, Event Type, Financial Period. An event that cannot be processed stays in the queue.
- Validation (PGL 1.5.11): Company; Branch; Product; Currency; Financial Period; Accounting Rule Availability; User Authorization.
- Recovery: automatic rollback; persistent queue; journal reprocessing; event replay; duplicate detection; commit verification.
- **[IMPL]** Event statuses (the doc names "Event Status" but does not list values): `RECEIVED → VALIDATED → RULE_RESOLVED → JOURNAL_CREATED → POSTED → COMPLETED`, with side states `FAILED` (retryable), `REJECTED` (validation), `ON_HOLD` (e.g., period closed) and `REVERSED`.
- PGL 2.4 Part B (lifecycle, controls, security, audit trail, event reporting) is headings only.
- **[IMPL]** Screens: Event master; Event monitor/queue (filter by status, retry, replay); Event detail with drill to journal. Reports: event register; failed-event report; unprocessed-event aging.

### 4.6 Example Dr/Cr entries

**Given in the doc (PGL 2.3 "GL Mapping", verbatim):**

| Transaction | Accounts named (order as given) | Read as |
|---|---|---|
| Premium Collection | Bank Account & Premium Income | Dr Bank / Cr Premium Income |
| Claim Payment | Claims Expense & Bank Account | Dr Claims Expense / Cr Bank |
| Commission Payment | Commission Expense & Commission Payable | Dr Commission Expense / Cr Commission Payable |
| Reinsurance Premium | Reinsurance Expense & Reinsurance Payable | Dr RI Expense / Cr RI Payable |
| GST Collection | GST Payable Account | Cr GST Payable (Dr side = Bank/Receivable) |

The doc provides no other Dr/Cr examples.

**[IMPL] Default rule templates for FinVerse.** These are standard non-life insurance accounting, to be seeded as configurable rules:

| Event code | Dr | Cr |
|---|---|---|
| POLICY_ISSUE (premium raised, credit business) | Premium Receivable (policyholder/broker/agent sub-ledger) | Gross Written Premium Income; Tax/GST Payable; Stamp Duty/Levy Payable |
| POLICY_ISSUE (cash business) | Bank/Cash | Premium Income; GST Payable |
| POLICY_RENEWAL | same as POLICY_ISSUE | |
| ENDORSEMENT (additional premium) | Premium Receivable | Premium Income; GST Payable |
| ENDORSEMENT (return premium) | Premium Income; GST Payable | Premium Refund Payable / Premium Receivable |
| POLICY_CANCELLATION | Premium Income (unexpired portion); GST Payable | Premium Refund Payable / Premium Receivable |
| PREMIUM_RECEIPT | Bank/Cash | Premium Receivable (or Premium Deposit/Unallocated Receipts if unmatched) |
| PREMIUM_REFUND | Premium Refund Payable | Bank |
| COMMISSION_PAYABLE (accrual) | Commission Expense (Agent/Broker/Override) | Commission Payable (intermediary sub-ledger); Withholding Tax Payable |
| COMMISSION_PAYMENT | Commission Payable | Bank |
| COMMISSION_RECOVERY (on cancellation) | Commission Payable/Receivable | Commission Expense |
| CLAIM_REGISTER / CLAIM_RESERVE | Claims Incurred – Change in OCR (Expense) | Outstanding Claims Reserve (Liability) |
| CLAIM_RESERVE revision | ± Change in OCR | ± OCR |
| CLAIM_APPROVE (settlement) | OCR | Claims Payable |
| | Claims Paid Expense | Claims Incurred – Change in OCR |
| | (simplified: Dr Claims Expense / Cr Claims Payable) | |
| CLAIM_PAYMENT | Claims Payable (or Claims Expense per doc) | Bank |
| CLAIM_RECOVERY / SALVAGE | Bank / Recovery Receivable | Claims Recoveries (contra-expense) |
| REINSURANCE_PREMIUM (treaty/fac cession) | RI Premium Ceded (Expense) | Reinsurance Payable (reinsurer sub-ledger) |
| RI_COMMISSION | Reinsurance Payable / RI Receivable | RI Commission Income |
| REINSURANCE_RECOVERY (claim share) | Reinsurance Recoverable | Claims Recovered from RI (contra-expense) |
| RI_SETTLEMENT | Reinsurance Payable (net) | Bank (or Dr Bank / Cr RI Recoverable) |
| COINS share (lead insurer) | Coinsurer Receivable/Payable | Premium/Claims share accounts |
| BANK_RECEIPT (misc) | Bank | Mapped income/receivable |
| BANK_PAYMENT (vendor) | Vendor Payable / Expense | Bank |
| UPR (period-end) | Change in UPR (P&L) | UPR (Liability); reverse/re-compute each period |
| DAC | DAC (Asset) | Change in DAC (P&L) |
| IBNR/IBNER/PDR | Change in reserve (P&L) | Reserve (Liability) |
| FX revaluation | Unrealized FX Loss / Monetary account | Monetary account / Unrealized FX Gain |
| FX settlement | Realized FX Loss or Dr account | Realized FX Gain |
| Inter-company | Due-From Company X (in A's books) | Due-To Company A (in X's books); reciprocal P&L/BS lines |
| Year-end close | Revenue accounts (to zero) | Expense accounts (to zero); net to Retained Earnings / Current Year Profit |

---

## 5. Accounting Rule Configuration (PGL 1.2.4, 1.3.3.4, 1.6.9, 1.7, 2.5)

- **5.1 Rule content:** A rule defines (PGL 1.2.4, verbatim): Accounting Event; Voucher Type; Debit General Ledger Account; Credit General Ledger Account; Cost Centre; Profit Centre; Currency; Branch; Company; Posting Sequence; Narration.
  - Rule outputs (PGL 1.3.3.4, 1.6.9.2): Debit Account; Credit Account; Voucher Type; Posting Sequence; Posting Amount; Currency; Cost Centre; Profit Centre; Sub-Ledger; Financial Dimensions; Narration; Tax Treatment.

### 5.2 Rule structure and selection

- Rule repository structure (PGL 1.7.6): Accounting Event → Rule Definition → Condition Set → Account Mapping → Financial Dimensions → Currency Rules → Tax Rules → Posting Instructions → Version Information.
- Selection/condition parameters (union of PGL 1.3.3.4, 1.7.7, 2.5.6): Company; Branch; Product; Scheme; Policy Type; Line of Business; Event Code; Transaction Type/Category; Currency; Financial Period; Customer Category; Broker; Agent; Financial Amount; Business Conditions; Regulatory Requirements.
- Rule processing (PGL 1.7.5): Context Validation → Rule Selection → Condition Evaluation → Account Mapping → Amount Calculation → Financial Dimension Resolution → Journal Instruction Generation.
- Account determination may reference: Product Master; Account Mapping Rules; Company, Branch and Currency Configuration; Business Event Configuration; Financial Dimension Rules (PGL 1.6.9.3).
- Rule priority and sequence (PGL 2.5.7): heading only. **[IMPL]**
  - Most-specific match wins, scored by the number of non-wildcard conditions. Ties are broken by an explicit `priority`.
  - Each rule has N lines, each with `line_seq`, `dr_cr`, `account` (fixed or derived), `amount_expression` (component name or formula), dimension overrides and a narration template.
  - Rule selection must be deterministic. If no rule matches, the event fails with "Accounting Rule Availability".

- **5.3 Rule governance:** Each rule is uniquely identified and version-controlled with effective dates (PGL 1.7.6).
  - Rules are Configuration-Driven, Modular, Extensible, Reusable, Deterministic, Auditable (PGL 1.7.8).
  - PGL 2.5 Part B (account mapping, rule validation, version mgmt, security/authorization, audit/reporting) is headings only. **[IMPL]**
    - Rule status: `DRAFT → PENDING_APPROVAL → ACTIVE → SUPERSEDED/INACTIVE`, with maker-checker.
    - Rule validation: all accounts active, leaf-level and currency-compatible; debit total equals credit total by construction.
    - Provide a test/simulation mode: feed a sample event and preview the journal.

### 5.4 Screens and reports

- Screens: Rule master (header plus conditions plus Dr/Cr lines); Rule simulator; Rule version history/compare; Event-to-rule mapping matrix.
- Reports: Rule register; rules by event; rule change audit; unmapped events.

---

## 6. Journal Processing (PGL 1.8, 3.1, 2.2; GLM TP)

### 6.1 Journal types

- PGL 3.1.6 (verbatim): Premium Journal; Endorsement Journal; Claims Journal; Reinsurance Journal; Commission Journal; Payment Journal; Receipt Journal; Investment Journal; Adjustment Journal; Reversal Journal; Manual Journal.
- JV entry kinds (PGL 2.2): Manual Journal Entries; System Generated Entries; Adjustment Entries; Reversal Entries; Accrual Entries; Correction Entries.
- Other journal kinds used elsewhere in the doc: Closing Journal, Opening Balance Journal (3.10), Revaluation Journal (3.4.9), Settlement Journal (3.5.9), Elimination and Consolidation Adjustment Journals (3.6), Reconciliation Adjustment Journal (3.7.9).
- GLM transfer types: Account to Account, Account to GL, GL to GL. Provision vouchers are allowed for P&L accounts (GLM 31, 46).
- GLM batch shapes: 1Dr–1Cr, 1Cr–nDr, 1Dr–nCr, nDr–nCr.
- **[IMPL]** Journal Type master fields: code; name; numbering series (per company/branch/FY); source (SYSTEM/MANUAL); approval required; approval matrix ref; allowed accounts; reversible flag; auto-reverse-next-period flag (for accruals).

- **6.2 Journal header fields:** Journal Number; Journal Date; Accounting Period; Company; Branch; Currency; Source Module; Business Event; Journal Status; Reference Number; Description (PGL 3.1.3).
  - Plus: Posting Status; Voucher Number; Voucher Date; Tax Details; Audit Metadata (PGL 1.3.3.5, 1.8.4).
- **6.3 Journal line fields:** Line Number; GL Account; Debit Amount; Credit Amount; Currency; Exchange Rate; Cost Centre; Profit Centre; Department; Product; Business Unit; Transaction Reference; Narration (PGL 3.1.3, 1.8.4).
  - **[IMPL]** Also: base-currency Dr/Cr; reporting-currency amounts; sub-ledger party type and id; policy/claim/treaty refs.

### 6.4 Generation steps (PGL 3.1.4)

1. Receive validated Accounting Event
2. Identify Accounting Rule
3. Determine Dr/Cr Accounts
4. Calculate amounts
5. Assign Financial Dimensions
6. Generate Header
7. Generate Lines
8. Validate balance
9. Store in Journal Repository
10. Submit for Posting

- **6.5 Statuses:** The doc names a Processing, Validation, Authorization and Posting Status (PGL 1.8.4). Named states are "Ready for Posting" (1.8.8), "Posting Failed" (3.2.9), Cancelled (3.1.12), Posted and Reversed (3.1.14 report names).
  - **[IMPL]** Unified journal status: `DRAFT → VALIDATED → PENDING_APPROVAL → APPROVED (Ready for Posting) → POSTED`.
    - Side states: `REJECTED` (returned to maker), `RETURNED`, `CANCELLED` (pre-posting only), `POSTING_FAILED` (repostable), `REVERSED` (posted, then offset by a reversal journal).
  - GLM: manual batches can be saved untallied in "Batch Processing" mode but must be tallied, saved and authorized before EOD. The standard entry screen cannot save or close an untallied batch.
- **6.6 Validation:** Header completeness; line completeness; valid GL accounts; Dr = Cr; dimensions; period status; company/branch validity; currency config; rule compliance; duplicate detection; mandatory fields; referential integrity (PGL 3.1.8, 1.8.8).
  - **[IMPL]** Also: leaf account only; account allows manual posting (for manual journals); back-dated and forward-dated window check (value-date limits set globally, per branch and per transaction code; GLM TP 2).
- **6.7 Approval workflow:** PGL 3.1.9: Journal Generated → Validation Completed → Maker Review → Checker Approval → Financial Authorization → Journal Released → Posting Processing → GL Update.
  - Approval depends on journal type and authorization limits. See §19.

### 6.8 Reversal, amendment and cancellation

- **Reversal:** mirrors the original with Dr and Cr swapped. History is never deleted.
  - Scenarios: Incorrect Accounting Entries; Policy Cancellation; Endorsement Reversal; Claim Recovery; Premium Refund; Duplicate Journal; Financial Adjustments (PGL 3.1.10).
  - GLM: the reversal voucher is system-generated when an authorized reversal is made.
  - **[IMPL]** A reversal links `reversal_of_journal_id`, can be dated in the current open period or on the original date (if that period is open), and is allowed only once per journal.
- **Amendment** (before posting only): allowed for description, reference no, dimensions, attachments and narrative. Amounts and GL accounts are locked after approval (PGL 3.1.11).
- **Cancellation** (unposted only):
  - Reasons: incorrect source; business withdrawal; duplicate; validation failure; authorization rejection.
  - Cancelled journals stay in the system for audit.
- **Copy:** a "copy tran" feature copies a historical transaction into a new journal (GLM TP 4).

- **6.9 Audit:** Captures: Journal No; Source Transaction; Business Event; User; created date/time; modification history; approval details; reversal info; posting status; system logs.

### 6.10 Screens

- Manual Journal Entry (multi-line, tally indicator, attachments, copy-from)
- Journal Batch entry/upload
- Journal Approval inbox
- Journal Inquiry/detail (drill to source event)
- Journal Reversal
- Journal Cancellation
- Recurring/accrual templates **[IMPL]**

- **6.11 Reports:** Journal Register; Daily Journal Report; Unposted Journal Report; Posted Journal Report; Reversed Journal Report; Manual Journal Report; Journal Exception Report; Voucher Register; Day Book.
  - GLM transaction checklist, with filters: batch no, customer, inputter, authorizer, cutoff amount.

### 6.12 Inquiry filters

- Journal No; Accounting Date; Source Module; Business Event; Company; Branch; GL Account; Dimensions; Status; User.

---

## 7. Posting Engine (PGL 1.3.3.6, 1.9, 3.2)

- **7.1 Posting sequence:** PGL 3.2.4:
    1. Receive Approved Journal
    2. Verify Eligibility
    3. Validate Period
    4. Validate GL Accounts
    5. Update Debit Balances
    6. Update Credit Balances
    7. Update Dimensions
    8. Commit
    9. Mark Posted
    10. Posting Confirmation
  - PGL 1.3.3.6: Validated Journal → Period Validation → GL Account Validation → Posting Authorization → Update Journal Status → Update GL Transaction → Update Account Balance → Update Period Balance → Update Sub-Ledger → Create Audit Trail → Completed.
- **7.2 Posting types:** Online; Batch; Scheduled; Manual; Automatic; Bulk; Reposting (PGL 3.2.5).
  - Real-time posting gives an immediate GL update and is suited to online transactions.
  - Batch posting handles high volume within controlled windows.

### 7.3 Posting rules and invariants

- Rule 1: only journals that are approved, balanced and in an open period are posted.
- Rule 2: atomic unit of work. Transaction Begin → GL Update → Balance Update → Audit → Commit OR Rollback. There is no partial posting (PGL 1.9.6, 3.2.7).
- Rule 3: duplicate prevention by unique journal id plus a posting-status check. Reposting runs the same validations and posts only once (PGL 3.2.10).
- Rule 4: GL transactions are immutable after posting.
- Rule 5: each GL transaction keeps references to its journal, business event and operational transaction.
- **[IMPL]** Use optimistic locking or row-level locks on balance rows. Assign posting numbers by gapless sequence per company/FY.

- **7.4 Posting validation (PGL 3.2.8, 1.9.8):** Journal approval; journal balance; GL account validity and status; company; branch; dimensions; period open; currency; duplicate posting; authorization; reference integrity; financial control validation.
- **7.5 Balances maintained:** Types: Account; Company; Branch; Financial Period; Currency; Cost Centre; Profit Centre; Dimension balances (PGL 1.6.11.2, 1.9.4).
  - Periodicity: Daily; Monthly; YTD; Opening; Closing (PGL 3.2.3).
  - **[IMPL]** Balance table grain: company × branch × account × currency × period × dimension-hash. Columns: opening, period_dr, period_cr, closing (txn ccy plus base ccy).
- **7.6 Posting exceptions:** Exceptions (PGL 3.2.9, verbatim): Invalid GL Account; Closed Financial Period; Inactive Company; Inactive Branch; Missing Financial Dimensions; Currency Conversion Failure; Duplicate Posting Request; Database Failure; System Timeout; Authorization Failure.
  - On exception the engine stops, rolls back, records the error, writes an exception log, notifies the user and sets the journal to "Posting Failed".
  - Recovery scenarios: DB connectivity restored; network recovery; system restart; master-data correction; authorization resolved; accounting period reopening.
- **7.7 Posting audit:** Captures: Journal No; Posting No; date/time; Company; Branch; User; Posting Method; Status; Errors; Recovery Actions; System Logs.
- **7.8 Monitoring:** Online and batch status; queue; failed; pending; performance metrics; throughput; processing time; exception dashboard.
- **7.9 Reports:** Posting Register; Daily Posting Report; Batch Posting Report; Failed Posting Report; Pending Posting Report; Reposted Journal Report; Posting Exception Report; Posting Performance Report; Posting Audit Report.

### 7.10 Screens

- Posting monitor (queue, failed list with repost action); Batch posting run; Posting detail.

---

## 8. Financial Periods (PGL 3.3, 4.4; GLM 11, 27)

- **8.1 Calendar:** Calendar hierarchy: Financial Year → Accounting Quarter → Accounting Month → Accounting Period → Business Date → Transaction.
  - Fiscal or calendar year is set by parameter (GLM 11).
- **8.2 Period entity:** Fields: Financial Year; Accounting Period Code; Period Description; Start Date; End Date; Period Status; Company; Branch; Currency; Closing Date.
  - **[IMPL]** Also: adjustment period flag (period 13 for audit adjustments); status by module (GL, AR, AP, Claims) for staged close.
- **8.3 Statuses and lifecycle:** Statuses (PGL 3.3.4, verbatim): **Future, Open, Active, Closing, Closed, Reopened, Archived**.
  - **[IMPL]** Semantics:
    - **Future:** defined, but no posting allowed. Forward-dated posting is allowed only if the policy permits.
    - **Open / Active:** normal posting. "Active" is the current default period for system events.
    - **Closing = soft close:** system/sub-ledger events and approved adjustment and accrual journals are allowed. Ordinary manual journals are blocked. Close checklist in progress.
    - **Closed (hard close):** no create, approve or post (PGL 3.3.9).
    - **Reopened:** temporarily open for authorized adjustments only, restricted to named users. It must be re-closed.
    - **Archived:** read-only historical.
  - Transitions: Future → Open → Active → Closing → Closed → (Reopened → Closed)* → Archived.
- **8.4 Opening:** Create period; validate calendar; initialize opening balances; activate; enable processing; configure period controls.
- **8.5 Controls:** Open-period validation; closed-period restriction; future-period restriction; back-dated posting control; forward-dated posting control; company-level and branch-level control; user authorization (PGL 3.3.6).
  - GLM "Supplementary Module": back- and future-dated transactions follow eligibility criteria. Balance Sheet and P&L reports can include or exclude supplementary transactions.
- **8.6 Period-end activities:** Verify pending journals; complete posting; resolve posting exceptions; GL reconciliation; verify TB; review suspense accounts; accruals; adjustments; generate reports; obtain financial approval (PGL 3.3.8).
  - **[IMPL]** Insurance-specific: run UPR, DAC and IBNR reserve postings; FX revaluation; auto-reverse prior-period accruals.
- **8.7 Close workflow:** PGL 3.3.9: Period Validation → Pending Transaction Verification → Journal Completion → Posting Completion → Financial Reconciliation → Management Approval → Period Close → Financial Statement Generation → Archive Period.
- **8.8 Reopening:** Reasons: audit adjustments; regulatory corrections; late entries; incorrect closing; data correction; system recovery.
  - Requirements: financial authorization; management approval; audit logging; controlled user access; revalidation.
  - Reopen flow (PGL 4.4.8): Request → Validate Business Justification → Management Approval → Finance Authorization → Reopen → Process Approved Adjustments → Revalidate → Reclose.
- **8.9 Audit:** Captures: FY; period; status; opening, closing and reopening dates; user; approvals; authorization history; timestamp; comments; exception history.
- **8.10 Screens:** Financial Year setup; Period calendar generator; Period status console (per company/branch, with transition buttons); Close checklist; Reopen request/approval.

### 8.11 Reports

- Financial Calendar Report; Open Period Report; Closed Period Report; Period Status Report; Period Closing Checklist; Period Reopening Report; Financial Reconciliation Report; Trial Balance Report; Period-End Exception Report; Year-End Preparation Report.

---

## 9. Multi-Currency (PGL 3.4, 1.10; GLM 7, 14, 25, 32; GLS)

- **9.1 Components:** Base Currency (per company); Transaction Currency; Reporting Currency; Exchange Rate Type; Exchange Rate; Rate Date; Currency Precision; Rounding Rules.
  - A single GL can hold balances in multiple currencies. Allowed currencies are set per GL (GLS "Currencies Allowed for a GL"). Posting controls can be set per currency.
- **9.2 Currency master:** Fields: Currency Code; Name; Symbol; Decimal Precision; Number Format; Status; Effective Date.
  - Only active currencies can be used.

### 9.3 Rate types (PGL 3.4.5, verbatim)

- **Spot Rate, Standard Rate, Average Rate, Month-End Rate, Historical Rate, Custom Rate.**
- A "Currency Rate Type Master Maintenance" screen exists (GLS).
- **[IMPL]** Rate table fields: from_ccy; to_ccy; rate_type; effective_date; rate; source; status; entered_by/authorized_by (maker-checker).
- **[IMPL]** Usage mapping:
  - Spot for transactions
  - Month-End (closing) for revaluation of monetary items and balance-sheet translation
  - Average for P&L translation in consolidation
  - Historical for equity/non-monetary items

### 9.4 Conversion steps

1. Receive transaction currency
2. Identify base currency
3. Retrieve rate
4. Validate rate
5. Calculate amount
6. Apply rounding
7. Generate journal
8. Store currency info
9. Post

- **[IMPL]** Post any rounding difference to a Rounding Difference account so the journal balances in base currency.

- **9.5 Validation:** Active currency; valid rate; effective date; currency pair; decimal precision; company currency; reporting currency.
- **9.6 FX gain and loss:** Realized FX Gain; Realized FX Loss; Unrealized FX Gain; Unrealized FX Loss; Exchange Difference Adjustments; Currency Settlement Variance.

### 9.7 Revaluation

- Accounts revalued: Bank Accounts; Premium Receivables; Claims Payables; Reinsurance Balances; Broker Balances; Supplier Accounts; Customer Accounts.
- Steps: Identify outstanding FC balances → latest rate → revalued amount → exchange difference → Revaluation Journal → update GL → Revaluation Report.
- The original transaction values are never changed. Differences go in separate adjustment journals.
- **[IMPL]** Revaluation run fields: company; period; rate type; account selection (accounts with revaluation_flag); status (DRAFT/POSTED/REVERSED). Auto-reverse on day 1 of the next period (reversal method) is configurable.

- **9.8 Exceptions:** Missing Exchange Rate; Invalid Currency Code; Inactive Currency; Expired Exchange Rate; Invalid Currency Pair; Rounding Variance; Currency Precision Error; Revaluation Failure.
  - On exception: stop, log, notify and **prevent GL posting**.
- **9.9 Audit:** Captures: txn/base/reporting ccy; rate; rate type; rate date; converted amount; adjustment; revaluation details; user; timestamp; reference.
- **9.10 Reporting currencies:** Transaction, Base, Reporting, Consolidated, Company, Branch; FC Exposure.
  - GLM: currency-wise balances are shown alongside base equivalents.
- **9.11 Reports:** Exchange Rate Register; Daily Exchange Rate Report; Foreign Currency Transaction Report; Currency Conversion Report; FX Gain/Loss Report; Currency Revaluation Report; Currency Exposure Report; Multi-Currency Trial Balance; Currency Audit Report; Currency Exception Report.

### 9.12 Screens

- Currency master; Rate type master; Exchange rate entry/upload (with authorization); Revaluation run; Currency inquiry.

---

## 10. Inter-Company and Inter-Branch (PGL 3.5; GLM 9, 15, 19, TP 10)

- **10.1 Components:** Source/Destination Company; Source/Destination Branch; Inter-Company Account; Due-To Account; Due-From Account; Transaction Reference; Settlement Status; Reconciliation Status.
- **10.2 Inter-Company master:** Fields: Company Code; Branch Code; Counterparty Company; Inter-Company GL Accounts; Settlement Method; Currency; Effective Date; Status.
  - Only authorized relationships may transact.

### 10.3 Process

1. Initiate
2. Identify source
3. Identify destination
4. Validate relationship
5. Generate Due-To/Due-From
6. Create journal
7. Post
8. Update reconciliation status
9. Await settlement

- **10.4 Entries:** Due-To Company Account; Due-From Company Account; Inter-Company Receivable; Inter-Company Payable; Settlement Adjustment; Elimination Entries (at consolidation).
  - **[IMPL]** A single IC event produces two balanced journals, one per company, linked by a shared `ic_reference`. Both post, or neither does.
- **10.5 Validation:** Company; branch; authorized relationship; IC GL accounts; currency; period (both sides open); duplicate check; settlement rule.
- **10.6 Reconciliation:** Match transactions; compare Due-To against Due-From; verify references; validate amounts; verify currency; identify unmatched; resolve; confirm status.
- **10.7 Settlement:** Flow: Identify outstanding → verify reconciled → settlement instructions → Settlement Journal → post → update Due-To/From → mark Settled → confirmation.
  - Frequency: daily, weekly, monthly or on demand.
  - **[IMPL]** IC transaction statuses: `OPEN → MATCHED/UNMATCHED → RECONCILED → SETTLED`, with `DISPUTED` as a side state.
- **10.8 Exceptions:** Invalid Company Relationship; Missing Inter-Company Account; Currency Mismatch; Financial Period Closed; Duplicate Transaction; Unbalanced Journal; Reconciliation Failure; Settlement Failure; Authorization Failure.
- **10.9 Inter-branch (GLM):** GL Posting Control Maintenance sets inter-branch limits and restrictions and which GLs other branches may post to ("GL Posting Controls for Other Branch GL").
  - Account freezing is available.
  - **[IMPL]** Inter-branch postings auto-generate HO/branch clearing (Inter-Branch Account) entries so each branch balances.
- **10.10 Audit:** Captures: source/dest company and branch; txn ref; journal no; settlement no; recon status; user; approvals; timestamp; reference.
- **10.11 Reports:** Inter-Company Transaction Register; Due-To Balance Report; Due-From Balance Report; Inter-Company Reconciliation Report; Outstanding Settlement Report; Settlement History Report; Unmatched Transaction Report; Inter-Company Trial Balance; Inter-Company Audit Report; Exception Report.

### 10.12 Screens

- IC relationship master; IC transaction entry; IC matching workbench; Settlement run; IC inquiry.

---

## 11. Consolidation (PGL 3.6)

- **11.1 Components:** Parent Company; Subsidiary Company; Business Unit; Branch; Consolidation Group; Consolidation Period; Consolidation Currency; Elimination Rules; Ownership Percentage; Consolidated Ledger.
- **11.2 Consolidation group:** Fields: Group Code; Parent Entity; Member Companies; Branch Structure; Ownership %; Reporting Currency; Effective Date; Group Status.
  - Only active companies in approved groups take part.

### 11.3 Process

1. Collect data
2. Validate period
3. Map CoA
4. Translate FX
5. Apply consolidation rules
6. Eliminate IC
7. Generate consolidated journals
8. Update Consolidated GL
9. Generate statements

- **11.4 CoA mapping:** Local account → group account; classification validation; dimension mapping; balance verification; mapping exceptions.
  - Every account used must map to the Group CoA.
- **11.5 Validation:** Company; group; period; TB balanced; mapping; translation; ownership structure; balance verification.
- **11.6 Eliminations (PGL 3.6.8, verbatim):** Eliminate Inter-Company Sales; Purchases; Receivables; Payables; Profit; Interest; Dividends; Internal Transfers.
  - Eliminations generate adjustment journals in the consolidated ledger and never modify company GLs.
- **11.7 Adjustments:** Consolidation Adjustment Journals; Manual Financial Adjustments; Ownership Percentage Adjustments; Minority Interest Adjustments; Currency Translation Adjustments; Reclassification Entries; Top-Level Consolidation Adjustments; Audit Adjustments.
- **11.8 Runs and statuses:** The doc names a Consolidation Run Number and Consolidation Status. A duplicate run for the same group and period is an exception.
  - **[IMPL]** Run statuses: `DRAFT → VALIDATED → ELIMINATED → FINAL (locked)`, plus `REVERSED`.
- **11.9 Exceptions:** Missing Financial Data; Closed Accounting Period; Invalid Consolidation Group; CoA Mapping Failure; Currency Translation Error; Inter-Company Balance Mismatch; Ownership Structure Error; Consolidation Rule Failure; Duplicate Consolidation Run.
  - On exception, statement generation is blocked.
- **11.10 Audit:** Captures: group; parent; participants; period; run no; elimination/adjustment journal refs; user; approvals; timestamp; status.
- **11.11 Reports:** Consolidated Trial Balance; Consolidated Balance Sheet; Consolidated P&L; Consolidated Cash Flow; Inter-Company Elimination Report; Consolidation Adjustment Report; Ownership Structure Report; Currency Translation Report; Consolidation Audit Report; Consolidation Exception Report.

### 11.12 Screens

- Consolidation group master; Group CoA and mapping; Elimination rule master; Consolidation run console; Consolidation adjustments entry.

---

## 12. Reconciliation (PGL 3.7; GLM 18, 28, 44)

- **12.1 Types (PGL 3.7.4, verbatim):** General Ledger Reconciliation; Bank Reconciliation; Premium Reconciliation; Claims Reconciliation; Reinsurance Reconciliation; Broker Reconciliation; Accounts Receivable Reconciliation; Accounts Payable Reconciliation; Inter-Company Reconciliation; Suspense Account Reconciliation.
- **12.2 Components:** GL Balance; Subsidiary Ledger Balance; Bank Statement; Journal Entries; Adjustment Entries; Reconciliation Rules; Difference Amount; Reconciliation Status; Approval Details.

### 12.3 Process

1. Collect
2. Validate source
3. Compare balances
4. Identify differences
5. Analyze
6. Adjustment entries if needed
7. Approve
8. Update status
9. Report

- **12.4 Matching rules:** Account; Transaction Reference; Amount; Currency; Date; Company; Branch; Dimension; Tolerance Limits.
- **12.5 Validation:** GL; sub-ledger; bank statement; journal; currency; period; duplicates; balance.
- **12.6 Difference categories (PGL 3.7.8, verbatim):** Missing Transactions; Duplicate Transactions; Amount Differences; Timing Differences; Currency Conversion Differences; Incorrect GL Account Posting; Incorrect Financial Dimension Assignment; Bank Charges and Interest; Manual Posting Errors.
  - Each difference is classified, investigated, assigned to a responsible team and tracked to resolution.
- **12.7 Adjustment flow:** Identify → Root cause → Prepare Adjustment Journal → Validate → Financial Approval → Post → Reconcile updated balances → Close.
  - The adjustment journal links to the reconciliation record.
- **12.8 Bank reconciliation:** **[IMPL]** Import bank statements (CSV/MT940/CAMT.053). Auto-match on amount, date and reference within a tolerance, then manual match. Outputs: BRS (balance per bank vs per book, with unpresented cheques, uncleared deposits, bank charges/interest not booked).
- **12.9 Sub-ledger reconciliation:** **[IMPL]** Control-account balance vs the sum of AR/AP/Premium/Claims/RI/Broker sub-ledger balances, per company, branch, currency and period. Differences drill to event or journal.
- **12.10 Nominal GL auto-reconciliation (GLM):** Originating entries carry a nominal number and responding entries knock them off. Report outstanding entries by age.
- **12.11 Statuses:** **[IMPL]** Reconciliation statuses: `OPEN → IN_PROGRESS → DIFFERENCES_IDENTIFIED → PENDING_APPROVAL → RECONCILED/CLOSED`, plus `SUSPENDED` on exception.
  - Unreconciled items block period close (PGL 4.4.4).
- **12.12 Exceptions:** Missing Source Data; GL Balance Mismatch; Bank Statement Mismatch; Duplicate Transactions; Invalid GL Account; Currency Mismatch; Closed Accounting Period; Adjustment Validation Failure; Approval Failure.
  - On exception, reconciliation is suspended and **reconciliation closure is prevented**.
- **12.13 Audit:** Captures: Recon No; Type; Company; Branch; Period; Source System; GL Account; Difference; Adjustment Journal Ref; Status; User; Approval; timestamp.
- **12.14 Reports:** GL Reconciliation; Bank Reconciliation; Premium Reconciliation; Claims Reconciliation; Reinsurance Reconciliation; Broker Reconciliation; Inter-Company Reconciliation; Outstanding Difference; Reconciliation Audit; Reconciliation Exception.

### 12.15 Screens

- Bank statement import; Bank reconciliation workbench; Sub-ledger vs GL recon; Suspense clearing; Recon approval; Nominal GL outstanding.

---

## 13. Budgets (PGL 3.8)

- **13.1 Components:** Budget Year; Budget Version; Budget Category; Budget Period; Budget Amount; GL Account; Cost Centre; Department; Branch; Business Unit; Budget Status.
- **13.2 Budget master:** Fields: Budget Code; Description; Financial Year; Company; Branch; Department; Budget Owner; Budget Currency; Budget Amount; Effective Date; Status.
  - Only approved budgets are used for monitoring.

### 13.3 Planning

1. Create
2. Define period
3. Allocate amounts
4. Assign GL accounts
5. Assign dimensions
6. Validate
7. Submit
8. Approve
9. Activate

- **13.4 Allocation methods:** Company-wise; Branch-wise; Department-wise; Cost Centre; GL Account; Business Unit; Monthly; Quarterly; Annual.
- **13.5 Validation:** FY; period; company; branch; GL account; dimension; duplicate budget; amount.
- **13.6 Statuses:** **[IMPL]** `DRAFT → SUBMITTED → APPROVED → ACTIVE → REVISED (new version) → CLOSED`, plus `REJECTED`.
- **13.7 Monitoring:** Availability check; consumption tracking; balance; utilization %; threshold monitoring; overrun detection; commitment tracking; availability reporting.
  - **[IMPL]** Budget control modes per budget: `NONE`, `WARN` or `BLOCK`. Applied at manual journal/expense validation when an account + cost-centre budget would be exceeded.
- **13.8 Variance:** Flow: Approved Budget vs Actual GL → comparison → calculation → classification → management review → corrective action → reporting.
  - Categories: Favourable; Unfavourable; Revenue; Expense; Capital Expenditure; Operational Cost.
- **13.9 Revision:** Scenarios: Additional Allocation; Reduction; Department Budget Transfer; Cost Centre Reallocation; Project Budget Revision; Emergency Approval.
  - Each revision requires justification, validation, management approval, version control and audit logging. Prior versions are kept.
- **13.10 Audit:** Captures: Budget Code; Version; FY; Company; Branch; Dept; GL; Amount; Revised Amount; Status; User; Approval; timestamp.
- **13.11 Reports:** Budget Register; Budget Allocation; Budget Utilization; Budget Availability; Budget Variance; Budget Revision History; Department Budget; Cost Centre Budget; Budget Audit; Budget Exception; Budget vs Actual.

### 13.12 Screens

- Budget header/version; Budget line grid (account × dimension × period) with spread tools and upload; Budget approval; Budget revision/transfer; Budget vs actual inquiry.

---

## 14. Financial Reporting (PGL 1.3.3.8, 2.2, 3.9; GLM 25, 34, 36, 37, 40)

- **14.1 Principle:** Reports are built **only from posted transactions** (PGL 1.3.3.8).
- **14.2 Financial statements:** Trial Balance; Balance Sheet; Profit and Loss Statement; Cash Flow Statement; Statement of Changes in Equity; Budget vs Actual Statement; Consolidated Financial Statements; Financial Ratio Analysis (PGL 3.9.8).
- **14.3 Standard reports (PGL 3.9.4, 2.2):** Trial Balance; Balance Sheet; P&L; Cash Flow; General Ledger Report; Journal Register; Account Statement; Budget vs Actual; Financial Ratio Report; Consolidated Statements; Bank Book; Cash Book; Tax Reports.

### 14.4 Other report groups (PGL 1.3.3.8)

- **Operational:** Journal Register, Voucher Register, Day Book, Ledger Inquiry, Account Activity.
- **Management:** Branch Financial Reports, Company Consolidation, Cost Centre Reports, Profit Centre Reports, Budget Variance Reports.
- **Regulatory:** Statutory Financial Statements, Insurance Regulatory Reports, Tax Reports, External Audit Reports.
- **Regulatory outputs** (PGL 1.4.7): Statutory Financial Statements; Insurance Regulatory Reports; Premium Reports; Claims Reports; Reinsurance Reports; Tax Reports; Audit Reports.

- **14.5 GLM report features:** GL Reports in Base Currency; GL Transactions Report.
  - Branch, branch-list or entity scope. Reports can be run for any date including back-dated, and can include or exclude supplementary transactions. PDF output.
- **14.6 Parameters:** FY; Accounting Period; Company; Branch; Department; Cost Centre; Business Unit; GL Account Range; Currency; Report Format.

### 14.7 Process

1. Select period
2. Retrieve GL data
3. Validate balances
4. Apply template
5. Calculate
6. Generate
7. Review/approve
8. Distribute
9. Archive

- **14.8 Validation:** Period; GL balances; TB balanced; currency; company; branch; dimensions; completeness.
- **14.9 Exceptions:** Missing Financial Data; Unposted Journals; Trial Balance Not Balanced; Closed Reporting Period; Invalid Report Parameters; Currency Translation Failure; Report Generation Failure; Authorization Failure; Distribution Failure.
  - On exception, publication is blocked.
- **14.10 Distribution:** Flow: Generate → Validate → Approve → Distribution List → Publish → Notify → Archive → Distribution History.
  - Formats: PDF, Excel, CSV, HTML, Print.
  - Access is role-based.
- **14.11 Report templates:** **[IMPL]** A configurable Financial Statement Template (FSV) maps account ranges or reporting groups to statement lines, with subtotals and formulas. A separate template covers each regulator format (e.g., IRDAI/NAIC-style schedules). Templates are versioned.
- **14.12 Audit:** Captures: Report Name; Type; FY; Period; Company; Branch; Parameters; generated date/time; Generated By; Approval; Distribution; Report Version.
- **14.13 Report-admin reports:** Financial Statement Register; Report Generation History; Report Distribution Report; Report Execution Log; Reporting Audit Report; Reporting Exception Report; Report Usage Statistics; Scheduled Report Status; Report Access Log; Report Version History.

### 14.14 Screens

- Report catalogue/launcher; Report scheduler; Statement template designer; Report output archive; Distribution list master.

---

## 15. Year-End Closing (PGL 3.3.11, 3.10, 4.4.5; GLM 11, 21)

- **15.1 Components:** Financial Year; Closing Period; Closing Journal; Adjustment Journal; Retained Earnings Account; Opening Balance Journal; Closing Status; Financial Statements; Audit Information.
- **15.2 Year-end preparation (PGL 3.3.11):** Verify all periods complete; confirm GL balances; complete outstanding journals; finalize adjustments; complete reconciliations; generate TB; prepare statements; verify retained earnings; configure new FY.
- **15.3 Pre-closing (PGL 3.10.4):** Verify journal processing complete; posting complete; reconciliation complete; review suspense; post accruals; post adjustments; verify TB; validate statements; management approval.

### 15.4 Closing steps (PGL 3.10.5)

1. Validate Financial Year
2. Verify Completion of Accounting Activities
3. Generate Closing Journals
4. Close Revenue and Expense Accounts
5. Transfer Net Profit/Loss to Retained Earnings
6. Generate Opening Balance Journals
7. Initialize New Financial Year
8. Lock Closed Financial Year
9. Generate Year-End Reports

- **15.5 Closing entries:** Revenue Account Closing; Expense Account Closing; Profit Transfer Entry; Loss Transfer Entry; Retained Earnings Update; Opening Balance Creation; Balance Sheet Carry Forward; Closing Adjustment Entries.
  - Temporary (P&L) accounts are closed. Permanent (balance sheet) accounts are carried forward.
- **15.6 Opening balance generation:** Verify closed FY → identify permanent accounts → calculate closing balances → Opening Balance Journal → validate → post → initialize new FY → activate period.
- **15.7 Validation:** Period; TB; journal completion; posting completion; statements; reconciliation; opening balance; management approval.
- **15.8 Year-end controls (PGL 4.4.5):** All periods complete; statements validated; retained-earnings transfer confirmed; opening balances verified; FY locked; records archived; new FY prepared.
- **15.9 Exceptions:** Unposted Journals; Incomplete Financial Reconciliation; Trial Balance Not Balanced; Missing Financial Statements; Invalid Opening Balance; Closed Period Validation Failure; Retained Earnings Calculation Error; Authorization Failure; Duplicate Year-End Closing Attempt.
  - On exception, closure is blocked.
- **15.10 FY reopening:** Requires Executive Authorization, Finance Approval, Audit Approval, Reason, controlled access, full audit logging and revalidation.
  - **[IMPL]** On reopen, the closing and opening journals are reversed and regenerated when the FY is re-closed.
- **15.11 Retained earnings setup:** **[IMPL]** Per company config: Retained Earnings account; Current Year Profit account; P&L summary account. Opening balances are per account × branch × currency × (optionally) dimension.
- **15.12 Audit:** Captures: FY; Closing Run No; closing date/time; Opening Balance Journal No; Closing Journal No; Adjustment Journal No; Retained Earnings Entry; user; approvals; status; exceptions.
- **15.13 Reports:** Year-End Closing Report; Financial Year Closing Checklist; Closing Journal Report; Opening Balance Report; Retained Earnings Report; Year-End Trial Balance; Final Balance Sheet; Final P&L; Year-End Audit Report; Year-End Exception Report.

### 15.14 Screens

- Year-end close wizard (checklist, validations, preview closing journal, execute); FY reopen request; Opening balance review.

---

## 16. GL Inquiry (PGL 3.11; GLM 33, 38, 42)

- **16.1 Principles:** Inquiry is read-only, role-scoped and logged.
- **16.2 Inquiry types (PGL 3.11.4, verbatim):** General Ledger Account Inquiry; Journal Inquiry; Account Balance Inquiry; Trial Balance Inquiry; Transaction History Inquiry; Financial Period Inquiry; Budget Inquiry; Inter-Company Inquiry; Multi-Currency Inquiry; Audit Inquiry.
  - Also named: Voucher Inquiry; Account Activity (PGL 1.3.3.1).
- **16.3 Search criteria:** FY; Period; Company; Branch; Department; Cost Centre; Business Unit; GL Account; Journal No; Transaction Reference; Currency; Posting Status; User.

### 16.4 Workflow

1. Initiate
2. Authorize
3. Capture criteria
4. Retrieve
5. Filter
6. Calculate balances
7. Display
8. Export/print
9. Log inquiry

- **16.5 Drill-down:** Financial Statement → GL Balance → GL Account → Journal Entry → Journal Line → Source Transaction → Supporting Reference → Audit Information.
  - **[IMPL]** Also drill to policy, claim or treaty in the source module.
- **16.6 360-degree view (GLM 38):** A 360-degree client view (bank-oriented). **[IMPL]** Adapt it as a 360-degree Party view (policyholder/broker/agent/reinsurer): outstanding receivables/payables, transaction history, policies, claims, commissions.
- **16.7 Validation:** Authentication; role; company access; branch access; period; GL account; parameters; data availability.
- **16.8 Exceptions:** Unauthorized User Access; Invalid Search Criteria; Invalid GL Account; Closed Financial Period; Data Not Available; Company Access Restriction; Branch Access Restriction; Database Connectivity Failure; System Timeout.
- **16.9 Audit:** Captures: Inquiry No; Type; User ID; Role; Company; Branch; parameters; timestamp; accessed records; export activity; print activity.
- **16.10 Performance:** Indexed searches; optimized queries; cached reference data; filter-based retrieval; incremental loading; **pagination**; background processing for complex inquiries; response-time monitoring.
- **16.11 Reports:** General Ledger Inquiry Log; Journal Inquiry Report; Account Balance Inquiry Report; Transaction History Report; Inquiry Usage Statistics; User Activity Report; Inquiry Audit Report; Inquiry Exception Report; Data Access Report; Export Activity Report.

### 16.12 Screens

- Account inquiry (balances by period with drill); Account statement/ledger; TB inquiry (expandable hierarchy); Journal search; Party 360; Audit inquiry.

---

## 17. Dashboards and KPIs (PGL 3.12)

- **17.1 KPIs (PGL 3.12.4, verbatim):** Revenue; Expenses; Gross Profit; Net Profit; Operating Margin; Budget Utilization; Accounts Receivable Balance; Accounts Payable Balance; Cash Position; Working Capital; Current Ratio; Profitability Ratio.
  - **[IMPL]** Insurance KPIs, not in the doc but expected: GWP; NEP; Loss Ratio; Expense Ratio; Combined Ratio; Commission Ratio; RI Cession %; Premium Receivable Aging (>30/60/90); Outstanding Claims Reserve; Solvency proxies.
- **17.2 Dashboard components (PGL 3.12.6):** Executive Summary; Revenue Analysis; Expense Analysis; Budget Performance; Profitability Analysis; Cash Position; Trial Balance Summary; Balance Sheet Highlights; P&L Highlights; Financial Trend Charts; Alerts and Notifications.

### 17.3 Workflow

1. Collect
2. Validate
3. Calculate KPIs
4. Trend analysis
5. Widgets
6. User preferences
7. Display
8. Drill-down
9. Refresh

- **17.4 Trend analyses:** Revenue; Expense; Profitability; Cash Flow; Budget Utilization; GL Account Balance; Monthly; Quarterly; Year-over-Year; Multi-Year.
- **17.5 Alerts (PGL 3.12.9, verbatim):** Budget Threshold Exceeded; Negative Cash Balance; Unusual Revenue Variance; High Expense Variance; Large Journal Posting; Unbalanced Trial Balance; Failed Financial Reconciliation; Pending Period Closing; Failed Year-End Closing; Financial KPI Threshold Breach.
- **17.6 Components:** Financial Data Repository; Analytics Engine; KPI Library; Dashboard Templates; Report Widgets; Dimensions; Trend Module; Alert Engine; Executive Dashboard.
- **17.7 Validation:** GL balances; period; KPI calc; currency; company; branch; dimensions; completeness.
- **17.8 Exceptions:** Missing Financial Data; Invalid Dashboard Parameters; KPI Calculation Failure; Financial Period Not Available; Currency Conversion Error; Data Synchronization Failure; Unauthorized Dashboard Access; Report Generation Failure; System Performance Timeout.
- **17.9 Audit:** Captures: Dashboard Name; Request; User; Role; Company; Branch; parameters; KPI; period; drill-down; export; timestamp.
- **17.10 Reports:** Executive Financial Dashboard Report; Financial KPI Report; Revenue Trend Report; Expense Trend Report; Profitability Analysis Report; Budget Performance Report; Financial Ratio Analysis Report; Dashboard Usage Report; Analytics Audit Report; Analytics Exception Report.

### 17.11 Screens

- Executive dashboard; Finance ops dashboard (pending approvals, failed postings, open recon items, period status); KPI definition/threshold config; Alert subscriptions.

---

## 18. Security and Roles (PGL 1.5.19, 2.1.12, 4.1)

- **18.1 Components:** User Master; User Profile; User Role; User Group; Permission Set; Authentication Method; Session Management; Password Policy; Account Status; Login History.
- **18.2 User lifecycle:** Registration; Activation; Modification; Role Assignment; Password Reset; Temporary Lock; Deactivation; Reactivation; Permanent Removal.
  - **[IMPL]** User statuses: `PENDING_ACTIVATION, ACTIVE, LOCKED, INACTIVE, REMOVED`.
- **18.3 Roles (PGL 4.1.5, verbatim):** System Administrator; Finance Administrator; Finance Manager; Accountant; Accounts Executive; Auditor; Branch Finance User; Read-Only User.
  - Approval-level personas (PGL 4.2.4): Maker; Checker; Approver; Finance Manager; Financial Controller; CFO; System Administrator (administrative activities).
  - GLM also names Teller and Head Teller for cash/vault. This is bank-specific. **[IMPL]** Map these to Cashier / Chief Cashier.

### 18.4 Permissions (PGL 4.1.8, verbatim)

- Create Records; Modify Records; Delete Records; Approve Transactions; Post Journals; View Financial Reports; Export Data; Print Reports; Administration Functions.
- Least privilege applies.
- Authorization domains (PGL 1.5.19.3): Journal Creation; Journal Authorization; Posting; Reversal; Financial Inquiry; Master Data Maintenance; Period Closing.
- **[IMPL]** Permission = function (screen/API) × action × data scope (company list, branch list, department, business unit).

- **18.5 Authentication:** User ID/Password; MFA; LDAP/AD; SSO; Password Expiry Policy; Account Lockout Policy; Session Timeout. Service-to-service/API auth (PGL 1.5.19.2).
- **18.6 Security validation:** Identity; account status; password validity; role; permissions; company access; branch access; session validity.
- **18.7 Session controls:** Secure login; auto timeout; concurrent session control; forced logout; session renewal; inactive termination; login history; device and IP logging.
- **18.8 Data protection:** Encryption at rest and in transit; key management; DB access control; secure backup; integrity verification.
- **18.9 Security audit:** Captures: user; login/logout; auth status; password changes; role changes; permission changes; failed logins; lock/unlock; admin activity.
- **18.10 Reports:** User Master Report; User Role Report; User Permission Report; Login History Report; Failed Login Report; Locked User Report; Password Expiry Report; Security Audit Report; User Activity Report; Security Exception Report.

### 18.11 Screens

- User master; Role master; Permission matrix editor; User-org scope assignment; Password policy; Session monitor.

---

## 19. Authorization Matrix and Approval Workflow (PGL 3.1.9, 4.2)

- **19.1 Components:** Authorization Matrix; Approval Workflow; User Role; Approval Level; Financial Limits; Delegation Rules; Escalation Rules; Approval Status; Approval History.
- **19.2 Levels:** Maker → Checker → Approver → Finance Manager → Financial Controller → CFO. System Administrator handles admin activities.
  - The number of levels is configured by transaction value, business rules and policy.

### 19.3 Workflow (PGL 4.2.5)

1. Initiate
2. Validate authority
3. Submit
4. Review
5. **Approve / Reject / Return**
6. Record decision
7. Proceed to posting or next stage
8. Audit

- **19.4 Delegation and escalation:** Temporary Delegation; Permanent Delegation; Alternate Approver; Escalation Based on Time; Escalation Based on Approval Level; Delegation Validity Period; Automatic Workflow Notification.
- **19.5 Authorization validation:** Identity; role; approval authority; **financial approval limit**; company access; branch access; transaction status; workflow status.
- **19.6 Compliance controls:** SoD; Maker-Checker; Role-Based Approval; Approval Limit Enforcement; Multi-Level Approval; Mandatory Audit Logging; Workflow Traceability; Controlled Delegation; Policy-Based Authorization.
- **19.7 GLM limits:** Transfer limits (Running Account Operation Parameter); teller limits for retention, receipts and payments; inter-branch limits (GL Posting Control).
  - **[IMPL]** Generalize these into "posting limits" per role × journal type × currency.
- **19.8 Authorization matrix:** **[IMPL]** Matrix row: company; branch (or *); document type (journal type / reversal / period reopen / FY reopen / budget / rule change / CoA change / rate entry / recon adjustment); currency; amount_from; amount_to; required levels (ordered roles); min approvers per level; SLA hours for escalation.
  - Hard rules:
    - The maker can never approve their own item.
    - One user cannot act at two levels on the same item.
    - Approval is evaluated on the base-currency amount.
  - Items requiring approval (from the doc):
    - Journals (per type/limit)
    - Period close and reopen (management plus finance)
    - FY reopen (executive plus finance plus audit)
    - Budget and budget revision
    - Reconciliation adjustments
    - Consolidation adjustments
    - Org entity lifecycle
    - CoA changes (higher-level authorization)
    - Configuration changes (admin authorization)
- **19.9 Approval statuses:** **[IMPL]** `PENDING → APPROVED / REJECTED / RETURNED`, plus `ESCALATED`, `DELEGATED` and `EXPIRED` (workflow timeout).
- **19.10 Exceptions:** Unauthorized Approval Attempt; Approval Limit Exceeded; Missing Approver; Invalid Workflow; Expired Delegation; Duplicate Approval; Rejected Transaction; Workflow Timeout; Incomplete Approval Chain.
- **19.11 Audit:** Captures: txn ref; workflow ref; level; status; approver ID; role; date/time; comments; delegation; escalation.
- **19.12 Reports:** Pending Approval Report; Approved Transaction Report; Rejected Transaction Report; Approval History Report; Approval Turnaround Time Report; Delegation Report; Escalation Report; Approval Audit Report; User Approval Activity Report; Approval Exception Report.

### 19.13 Screens

- Approval inbox (universal); Authorization matrix config; Delegation setup; Approval history viewer.

---

## 20. Audit Trail and Compliance (PGL 1.2.10, 4.3; GLM 8, 17, 24, 47)

- **20.1 Principles:** Every financial and non-financial transaction is audited, recording originator, modifier and authorizer with timestamps (GLM).
  - Audit records are immutable and tamper-resistant (PGL 4.3.2, 4.3.9).
- **20.2 Traceability fields (PGL 1.2.10):** Source Module; Source Document; Business Event; Accounting Event; Voucher Number; Posting Date; User ID; Authorization Details; Date and Time; Reversal Information.
- **20.3 Audit record fields (PGL 4.3.6):** Event Number; Event Type; User ID; Company; Branch; Transaction Reference; Action Performed; **Previous Value; New Value**; Processing Date and Time; System Reference.
- **20.4 Auditable activities (PGL 4.3.4, verbatim):** User Login and Logout; Journal Creation; Journal Modification; Journal Approval; Journal Posting; Period Opening and Closing; Budget Changes; Master Data Maintenance; Security Administration; Report Generation; System Configuration Changes.
  - Inquiries, exports and prints are also logged (PGL 3.11.9).
- **20.5 Compliance areas:** Accounting Standards; Financial Control Policies; Internal Audit; External Audit; Regulatory Reporting; SoD; Data Retention; Information Security.
- **20.6 Compliance monitoring:** Policy; regulatory; accounting standard; SoD monitoring; user access; approval workflow; financial period; audit-log integrity verification.
- **20.7 Audit validation:** Event authenticity; user identity; txn ref; timestamp; company; branch; completeness; integrity.
- **20.8 Retention:** Configurable retention periods; secure repository; archives; read-only history; controlled retrieval; long-term storage; integrity verification; secure disposal after retention.
- **20.9 Exceptions:** Missing Audit Record; Invalid Transaction Reference; Unauthorized Activity; Incomplete Audit Information; Duplicate Audit Entry; Audit Repository Failure; System Timestamp Error; Compliance Rule Violation; Data Integrity Failure.
- **20.10 Implementation:** **[IMPL]** Append-only `audit_log` table (or a separate store). Use a hash chain, where each row stores the hash of the previous row, for integrity verification. Write it in the same DB transaction as the business change. Keep JSON before/after diffs for master data.
- **20.11 Reports:** Audit Log Report; User Activity Report; Financial Transaction Audit Report; Security Audit Report; Compliance Monitoring Report; System Configuration Change Report; Approval Audit Report; Audit Exception Report; Regulatory Compliance Report; Audit Summary Report.

### 20.12 Screens

- Audit trail inquiry (by entity/record/user/date, with before/after diff); Compliance monitor (SoD violations, access reviews); Retention policy config.

---

## 21. Period-End Controls (PGL 4.4)

- **21.1 Components:** Accounting Period; Financial Year; Closing Checklist; Control Rules; Validation Rules; Approval Workflow; Exception Register; Closing Status; Reopening Authorization.
- **21.2 Period-end controls (PGL 4.4.4, verbatim):** Verify Journal Completion; Verify Posting Completion; Complete Financial Reconciliation; Validate Trial Balance; Review Suspense Accounts; Process Adjustment Entries; Verify Budget Updates; Obtain Management Approval.
- **21.3 Closing checklist items (PGL 4.4.6, verbatim):** Journals Posted; Reconciliations Completed; Adjustments Approved; Financial Reports Generated; Trial Balance Verified; Management Approval Obtained; Audit Review Completed; Closure Authorized.
- **21.4 Control validation before close:** Period status; FY status; **pending journals; pending approvals; pending reconciliations**; TB accuracy; statement completeness; user authorization.
- **21.5 Exceptions:** Pending Journal Entries; Pending Posting Activities; Unresolved Reconciliation Differences; Unbalanced Trial Balance; Missing Financial Statements; Unauthorized Closure Attempt; Incomplete Closing Checklist; Approval Failure; Reopening Validation Failure.
- **21.6 GLM end-of-day:** EOD halts while incomplete cash, clearing or transfer transactions exist. They must be completed or deleted first.
  - **[IMPL]** Provide a day-end/business-date close with the same check applied to unposted/untallied batches.
- **21.7 Checklist implementation:** **[IMPL]** Checklist items are master-configurable per company. Each item is either `AUTO` (a system check query with pass/fail) or `MANUAL` (attestation with user, date and comment). Close is enabled only when every mandatory item passes.
- **21.8 Audit:** Captures: FY; period; checklist ref; control activity; validation results; approvals; reopen request/authorization; user; timestamp.

### 21.9 Reports

- Period Closing Status Report; Year-End Closing Status Report; Closing Checklist Report; Financial Reconciliation Status Report; Period Reopening Report; Closing Approval Report; Control Audit Report; Control Exception Report; Financial Closing Summary Report; Compliance Status Report.

---

## 22. System Administration and Monitoring (PGL 1.5.16–1.5.20, 4.5; GLM TP 11)

- **22.1 Components:** System Configuration; Batch Scheduler; Monitoring Dashboard; Performance Monitor; Alert Manager; System Logs; Job Manager; Configuration Repository; Health Monitor.
- **22.2 Configuration areas:** Company; Financial Year; Currency; Batch Schedule; Notification Settings; System Parameters; Integration Parameters; Environment Settings.
  - Changes need administrator authorization and are audited.
- **22.3 Operational monitoring:** System Availability; Background Job Status; Batch Processing Status; Database Connectivity; Integration Status; User Activity; Resource Utilization; Service Availability.
- **22.4 Performance metrics:** Response Time; Journal Processing Throughput; Posting Performance; Batch Processing Duration; Database Performance; CPU; Memory; Storage.
- **22.5 Admin validation:** Admin authorization; config integrity; system availability; service status; job dependencies; resources; environment consistency; change authorization.
- **22.6 Alerts (PGL 4.5.8, verbatim):** System Service Failure; Background Job Failure; Batch Processing Failure; Database Connectivity Failure; Low Storage Space; High CPU Utilization; High Memory Utilization; Integration Failure; Security Alert; Performance Threshold Breach.
  - Channels: dashboard, email and other configured channels.
  - GLM Exception Codes Master defines exception conditions and raises alerts when parameters are breached. Reports and queries are provided for investigation.
- **22.7 Exceptions:** Service Unavailable; Background Job Failure; Database Connection Failure; Integration Failure; Configuration Error; Batch Execution Failure; Resource Exhaustion; Unauthorized Administrative Access; Performance Degradation.
  - The system detects, records, alerts, notifies and starts recovery where configured.
- **22.8 Audit:** Captures: Admin ID; activity; config changes; parameter changes; batch job execution; service restart; alert acknowledgement; session info; timestamp.
- **22.9 Non-functional requirements:** Horizontal scaling; queue partitioning; HA cluster with load balancer; fault isolation; DR site; backup and recovery.
  - **[IMPL]** Batch jobs to schedule:
    - batch posting
    - FX rate import
    - revaluation
    - reserve postings (UPR/DAC/IBNR)
    - auto-reversal of accruals
    - recon auto-match
    - IC settlement
    - dashboard/KPI refresh
    - scheduled reports
    - audit hash verification
    - archival
- **22.10 Reports:** System Health Report; Batch Processing Report; Job Execution Report; System Performance Report; Resource Utilization Report; Configuration Change Report; Alert History Report; Administration Audit Report; Operational Exception Report; System Availability Report.

### 22.11 Screens

- Admin dashboard; Job scheduler/manager (define, run, history, rerun); System parameters; Exception code master; Alert rules and acknowledgement; Integration status; Log viewer.

---

## 23. Cross-cutting: common exception-handling pattern

Every engine (posting, currency, IC, consolidation, reconciliation, reporting, year-end, inquiry, analytics, approval, audit, admin) follows the same pattern from the doc:

1. Stop or suspend processing.
2. Record the exception.
3. Generate an error log.
4. Notify the responsible user.
5. Prevent the downstream effect (posting, settlement, closure or publication).
6. Keep a complete audit record.

**[IMPL]** Use one `exception_log` entity with these fields: `id`, `module`, `exception_code` (from the Exception Code master), `severity`, `entity_type`, `entity_id`, `company`, `branch`, `message`, `details_json`, `status` (OPEN/ACKNOWLEDGED/RESOLVED), `assigned_to`, `created_at`, `resolved_by/at`. Every module "Exception Report" is a filtered view of this entity.

---

## 24. [IMPL] Consolidated FinVerse screen inventory (MVP ordering)

1. **Org and Setup:** Enterprise/Company/Region/Branch/Department/BU masters; Holiday calendar; System parameters.
2. **CoA:** GL Heads (tree), Sub GL, Micro GL, GL Category, Currencies allowed per GL, Access codes, Posting controls (own and other branch), GL closure/freeze.
3. **Dimensions:** Dimension types and values; Cost/Profit Centre; defaults; mandatory-by-account.
4. **Currency:** Currency master; Rate type master; Exchange rates; Revaluation run.
5. **Periods:** FY/period calendar; period status console; close checklist; reopen request.
6. **Accounting config:** Event master; Journal type/voucher series master; Accounting rule master plus simulator.
7. **Transactions:** Manual journal; batch journal/upload; approval inbox; reversal; cancellation; event monitor; posting monitor.
8. **Sub-ledgers:** AR (premium/broker/agent receivables), AP (claims/commission/RI/vendor payables) inquiry and aging; Bank and Cash book.
9. **Reconciliation:** Bank statement import and recon workbench; sub-ledger vs GL; suspense; IC matching; nominal GL outstanding.
10. **Inter-company and consolidation:** IC relationships; IC settlement; consolidation groups; group CoA mapping; elimination rules; consolidation run.
11. **Budget:** Budget entry/versions/approval/revision; budget vs actual.
12. **Reporting:** Report launcher (TB, BS, P&L, CF, SOCE, GL report, account statement, day book, journal/voucher registers, bank book, cash book, tax reports); statement template designer; scheduler/distribution.
13. **Year-end:** Close wizard; opening balances; FY reopen.
14. **Inquiry:** Account/balance/TB/journal/transaction history/party 360/audit inquiry.
15. **Dashboards:** Executive; Finance ops; KPI/alert config.
16. **Admin and security:** Users; roles; permissions; authorization matrix; delegation; password/session policy; job scheduler; exception codes; alerts; audit trail viewer; health dashboard.
