# iNXT BrokerVerse - BDOI Renewal (BRD-6, RN) Build Design

Status: **proposal for review**. This design extends `docs/architecture/BROKING_ARCHITECTURE.md`, `OPERATIONS_DESIGN.md`, `PRODUCT_MAINTENANCE_DESIGN.md` and the Developer Guide, which stay binding. It changes them only through the contract changes listed in section 13. The Collections (`COLLECTIONS_DESIGN.md`) and Accounting / Disbursement (`ACCOUNTING_DISBURSEMENT_DESIGN.md`) designs are authoritative for their modules; Renewal only reads from them.

It is aligned with the parallel designs merged on the branch: Submitted Policies (`SUBMITTED_POLICIES_DESIGN.md`, `RenewalHandOff` port and the shared account change V822), Employee Benefits (`EMPLOYEE_BENEFITS_DESIGN.md`, boundary EBQ28) and Claims (`CLAIMS_BROKING_DESIGN.md`, `ClaimExperienceQueryService`, CLQ28). Section 2.3 records the decisions.

Requirements baseline: [`BDOI_RN_BRD_SPEC.md`](../requirements/BDOI_RN_BRD_SPEC.md). It has 90 rows covering:
- BRRN.001-040;
- the main-BRD IDs 1.001-6.002;
- questions RQ01-RQ30.

Every class, migration and screen cites its BR ID in Javadoc or a comment, for example `BRRN.023` or `BRD 2.004.3`.

## 1. Design principles

1. **The candidate lives in Renewal, and the renewal becomes an account.**
   - Before the renewal is agreed, the expiring policy is a **renewal candidate**: extraction, checks, bucket, disposition, insurer decision, letters. This is new, and it lives in the new module `renewal`.
   - Once the renewal proceeds, it becomes an ordinary BRD-1 **account** of business type RENEWAL, linked to the expiring ARN. From there, placement, issuance and booking are the NB modules, unchanged except for the contract items of section 13.
   - Renewal never re-implements placement, issuance, booking or pricing.
2. **The ledger is the source of the expiring population.** Candidates are extracted from booked root invoices in `opsledger` (policy year, inception, expiry, PN, insurer, segment, AO, balances). Policy details come from the account.
   - Renewal stores the reference (expiring invoice no., ARN) and a small **snapshot** of the listing fields for reports.
   - It never stores a second copy of money. Outstanding premium, endorsements and payments are read live (BRRN.011 / 027: "sourced from the existing (mother) policy, not the extraction file").
3. **Rules are data, decisions are explained.**
   - The sanitation checks (BRRN.020) are code: one bean per check.
   - Their severity, the bucket rules (BRRN.023) and the decision matrix (BRRN.034) are versioned, maker-checker data.
   - Every bucket change, system disposition and override stores the rule version, the rule id, the check results and the user.
4. **Extraction is not initiation, and initiation is not automation.**
   - EXTRACTED candidates do nothing until an authorised user initiates them (BRRN.021).
   - After initiation, straight-through processing is allowed only for CLEAN candidates whose matrix rule says AUTO (BRRN.031 / 039).
   - Every stop is visible on the candidate as a failed check.
5. **Renewal posts no accounting.** The GL effect of a renewal is the BRD-1 booking of the renewal account (`BROKER_BOOKING`), with business type RENEWAL on the invoice (section 5).
6. **Parked means seam, not fake.**
   - The LAMD channel, insurer channels, legacy EBIX / QPS policies and the mail-domain policy each get a port or an upload.
   - Claims are read from the Claims module's public read API (`brokerclaims.service.ClaimExperienceQueryService`), not through a port of our own (section 2.3).
   - The default adapter is "not connected" or a manual upload.
7. **The addenda override the main BRD.** Where a BRRN restates a BRD ID, the BRRN's acceptance criteria apply (spec section 1).

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `renewal` (new; package `com.iortatechnxt.brokerverse.renewal`; tables `rnw_*`; screens `frontend/src/features/renewal`) | Extraction, candidates, check engine, buckets, decision matrix, initiation, assignment, transfer, disposition, TL review / post, overrides, processing worklist, dispositioned-file upload, insurer batches and responses, LAMD reports, letters (RA / NAL / NFR / NRNS / non-acceptance), acceptance, progression to placement and booking, follow-ups, reports, home, setup | BRRN.001-040, BRD 1.003-1.011, 2.003-2.009, 3.003-3.011, 4.003-4.09, 5.003-5.005 (seeds) | opsledger (read), account, catalog, crm (read), booking (read + queue port), placement (slip and hold-cover services), quotation, nonpackage, adjustment (read), brokerclaims (read), submitted (implements its `RenewalHandOff` port), workflow, lov, bulk, messaging, docgen, attachment, report, alert, system, security, audit, organization | V1010-V1017 (V1910-V1911) |
| `account` (BRD-1, built) | **Shared change V822** (account owner's range, agreed with Submitted Policies and Employee Benefits): `business_type`, `renewal_of_ref`, `NewAccount.renewal(...)`. Renewal adds only the `renewal_fast_track` transition and the RENEWAL rating purpose | BRRN.033/040, 3.007, 3.008 | unchanged | V822 (account range, shared); V1011 (transition row only) |
| `booking` (BRD-1, built) | Business type read from the account (part of the shared V822 change); Renewal adds `QueueSource.RENEWAL` | BRRN.040 | unchanged | V1011 (queue source check) |
| `quotation`, `nonpackage` (BRD-1, built) | `renewal_ref` origin on create; accounts created as RENEWAL | BRRN.033 | unchanged | V1011 |
| `report` (platform, built) | Parameter kind `CODE_SET` (include / exclude list) | BRD x.009.2, BRRN.003/019 | unchanged | none |
| `frontend/src/components/ui` (built) | New `GridTable`; new `components/broking/MultiSelectFilter` | BRRN.003/004/006/014-016 | - | - |

Why a new module rather than extending `account`:
- The candidate exists for every expiring policy, including the ones that are never renewed (Not for Renewal, Lost Business, NRNS).
- It carries its own workflow, rules and letters.
- If `account` carried them, it would depend on `opsledger`, `messaging` and `docgen` and would own the Marketing disposition, while the architecture keeps `account` as the risk record ("account knows nothing about quotations or placement").

### 2.1 Dependency graph (arrows = "depends on")

```
                          renewal (new)
      /      |       |        |         |          |           |        \
 opsledger account catalog  crm   booking(read,  placement   quotation  adjustment(read)
  (read)                          queue port)   (slip svc)   nonpackage
      |                                                               brokerclaims (read: ClaimExperienceQueryService)
      |                                                               submitted (renewal implements submitted.service.port.RenewalHandOff)
      \______ platform: workflow, lov, bulk, messaging, docgen, attachment, report, alert, system, security, audit ______/
```

No existing or planned module depends on `renewal`: `submitted` calls its own port `RenewalHandOff`, which `renewal` implements. Callbacks go through Spring events that `renewal` listens to:
- `AccountStatusChanged` (account);
- `InvoiceBooked` (booking);
- `InvoiceMovementPosted` (opsledger);
- `WorkCaseTransitioned` (workflow).

The graph stays acyclic, and ArchUnit needs no exception.

### 2.2 Ports declared in `renewal.service.port`

| Port | Default adapter | Implemented later by | Purpose |
|---|---|---|---|
| `LegacyPolicySource` | Bulk handler `RNW_LEGACY_POLICIES` only | Data migration | Expiring policies booked in EBIX / QPS before go-live (RQ27) |
| `RecipientPolicy` | Allows every well-formed address, requires `Protection` | Messaging change if BDOI confirms domain / TLS rules (RQ16) | RA negative scenario 6 |

Ports of other modules that `renewal` implements or calls:

| Port / API | Owner | Renewal's role | Purpose |
|---|---|---|---|
| `submitted.service.port.RenewalHandOff` | `submitted` (BRD Submitted Policies) | **Implements** it (`SubmittedPolicyRenewalHandOff`); the default `SubmittedRenewalHandOff` steps aside through `@ConditionalOnMissingBean` | Renewal of submitted-policy masterlist records (their SQ10; section 2.3) |
| `brokerclaims.service.ClaimExperienceQueryService.summary(arn, policyYear)` | `brokerclaims` (Claims BRD) | Calls it | Claim count, open count, statuses, paid, O/S per expiring ARN and policy year (BRD 1.003.4.1.1.22-23, BRRN.027/031/034) |

### 2.3 Boundaries with the parallel BRDs (decisions)

**Submitted Policies (`RenewalHandOff`, their SQ10).**
- **Decision: Renewal implements `RenewalHandOff` and owns the renewal of submitted policies from the hand-off onwards:** candidate, checks, disposition, renewal account creation, hold cover request, letters and progression to booking.
- `submitted` keeps what is specific to its masterlist:
  - intake, classification and buckets;
  - the expiry scan `SBM_EXPIRY_SCAN` with its lead days per segment (CBG Fire 150, CBG Motor 120);
  - the insurer assignment rules (`sbm_insurer_rule`), whose result travels in the hand-off request;
  - the masterlist status, which it updates from `AccountStatusChanged` and `InvoiceBooked` as designed.
- Renewal's adapter `SubmittedPolicyRenewalHandOff`:
  - creates an `rnw_candidate` with source `SUBMITTED_POLICY` and `source_ref` = SBM number; the candidate is idempotent on the SBM number;
  - initiates it at once, because the scan is the explicit initiation (BRRN.021);
  - runs the checks and the matrix;
  - creates the renewal account through `AccountService.createDraft(NewAccount.renewal(...))`, with `renewal_of_ref` = SBM number and the assigned insurer;
  - requests the 30-day hold cover through `HoldCoverService.request`. The insurer-acceptance follow-up and re-assignment (BRIDSP-32) stay in `placement` as Submitted Policies designed it;
  - queues the letters in the **single renewal letter engine** (`rnw_letter`): RA (generic or FFY template), reminder, SFU for mortgaged accounts, NRNS and NAL. Print and mail-house letters go through `submitted`'s `MailHouseGateway` port with channel PRINT.
- `sbm_letter` is then not written for renewals. One letter engine means one RA per client.
- **Non-CBG Retail "Renew with BDOI"** calls the same port, and the candidate starts at UNASSIGNED (manual).
- **No double extraction.** Submitted policies are not booked BIBS invoices, so `RNW_EXTRACTION` never finds them. Once the renewal is booked in BIBS, next year's renewal comes from `RNW_EXTRACTION`, and the masterlist record is closed as BOOKED.
- **Build order.** The adapter is built in wave R3, after the Submitted Policies S1-D wave. Until then, the default adapter of `submitted` runs. No data migrates between the two: open `sbm_renewal` rows finish on the default path.

**Account business type (one shared change).**
- The account change is the one designed by Submitted Policies in the account owner's range, `V822__account_business_type.sql`:
  - `acc_account.business_type` (NEW_BUSINESS / RENEWAL, default NEW_BUSINESS);
  - `renewal_of_ref` varchar(40);
  - `NewAccount.renewal(...)` and `Account.getBusinessType()`;
  - booking's `InvoiceBuilder` (line 129) reads the account's business type instead of the constant NEW_BUSINESS.
- Employee Benefits' variant (column in V1031 and a required `businessType` on `NewAccount`) is to be folded into V822 as well. Renewal proposes **no third variant**.
- For BIBS policies, `renewal_of_ref` holds the expiring **ARN**; for submitted policies it holds the SBM number. The candidate's `renewal_ref` and `renewal_arn` stay on `rnw_candidate`, not on the account.
- Renewal needs two small additions to that shared change. They are asked of the account owner and are not a separate migration:
  - an overload `NewAccount.renewal(..., Integer productVersionNo)`, so a renew-as-is keeps the expiring package version (PQ11);
  - `AccountPricing` rating with `RatingQuery.Purpose.RENEWAL` when the business type is RENEWAL.

**Employee Benefits (EBQ28).**
- EB programmes stay **out** of the general renewal lists:
  - `RNW_EXTRACTION` skips invoices whose product line is in `RNW_EXCLUDED_LINES`, which defaults to the EB benefit lines (`EB_BENEFIT_LINE`: HMO, GLI, GPA);
  - it also skips accounts linked to an EB cycle (their ARNs appear in `eb_cycle`, read through an EB query if one is exposed, otherwise by product line only).
- Every **Renewal Advice** Renewal produces is stored as an attachment of document type **`RENEWAL_ADVICE`** (the EB type, seeded idempotently by whichever migration runs first). It is linked to the renewal account, the client and the candidate, so the Customer Service Facility can resend EB and non-EB RAs alike. Other renewal letters use `RENEWAL_LETTER`.

**Claims (`ClaimExperienceQueryService`, CLQ28).**
- Renewal reads the loss experience from `brokerclaims.service.ClaimExperienceQueryService.summary(arn, policyYear)`: count, open count, statuses, paid and O/S. It feeds:
  - the list columns "Number of Claims" and "Status of Each Claim";
  - the CLAIMS check and the decision matrix;
  - the Account History tab.
- `renewal` depends on `brokerclaims` and never the reverse. If `brokerclaims` is not yet merged when R1 is built, the check looks the bean up with `ObjectProvider` and reports INFO "claims not connected".
- A **total-loss indicator** is not defined by the Claims BRD (CLQ28). The non-renewal reason "Total Loss Claim" (BRD 2.004.4.5) stays a manual reason, and the matrix condition TOTAL_LOSS is parked until CLQ28 is answered.

## 3. Flyway allocation

Allocated to this BRD: **schema V1010-V1019, demo V1910-V1919** (Developer Guide range table: 10 versions per later BRD). Eight schema versions are used (V1010-V1017), so one block is enough.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1010__renewal_foundation.sql` | R0 | Grants of the `RNW_*` permissions to existing roles; new roles `LAMD`, `CONTACT_CENTER`; LOV types and values (section 9); `sys_parameter` rows; workflow `RNW_CASE` (`wf_stage`, `wf_transition`); alert codes; notification events; document templates (drafts); `DOCUMENT_TYPE` values `RA_ACCEPTANCE`, `SIGNED_RA`, `LAMD_REPORT`, `INSURER_RENEWAL_FILE`, `RENEWAL_LETTER` and `RENEWAL_ADVICE` (insert ... on conflict do nothing, shared with Employee Benefits) |
| `V1011__renewal_contract_columns.sql` | R0 | `wf_transition` NB_ACCOUNT `DRAFT --renewal_fast_track--> AWAITING_PAYMENT` (system only); `bkg_queue` source check replaced to add `RENEWAL`; `quo_quotation.renewal_ref`, `npk_proposal.renewal_ref` varchar(30) nullable. **Not** the account columns: `acc_account.business_type` / `renewal_of_ref` are the shared `V822__account_business_type.sql` of the account owner (section 2.3), which must be applied first (V822 < V1011) |
| `V1012__renewal_candidates.sql` | R1-A | `rnw_extraction_run`, `rnw_candidate`, `rnw_check_run`, `rnw_check_result`, `rnw_bucket_history`, `rnw_remark`, `rnw_followup`, `rnw_override`, `rnw_history_view` |
| `V1013__renewal_rules.sql` | R1-A | `rnw_non_renewable_risk_code`, `rnw_check_setting`, `rnw_bucket_rule_set` / `rnw_bucket_rule`, `rnw_decision_matrix` / `rnw_decision_rule` |
| `V1014__renewal_disposition.sql` | R1-B | `rnw_disposition`, `rnw_transfer`, `rnw_candidate_endorsement` |
| `V1015__renewal_processing_insurer.sql` | R1-C | `rnw_insurer_batch`, `rnw_insurer_batch_line`, `rnw_insurer_response`, `rnw_lamd_report`, `rnw_lamd_line`, `rnw_upload_scope` |
| `V1016__renewal_letters.sql` | R1-D | `rnw_letter`, `rnw_letter_batch`, `rnw_acceptance` |
| `V1017__renewal_submitted_handoff.sql` | R3 | Templates `RNW_RA_FFY`, `RNW_SFU`; LOV values for the source SUBMITTED_POLICY; parameters of the hand-off |
| V1018-V1019 | - | Kept free for follow-ups |
| `db/demo/V1910__demo_renewal_setup.sql` | R0 | Demo users `lamd`, `contactc`, `rnwtl`; non-renewable risk codes; active bucket rule set and decision matrix (demo content); parameters for the demo |
| `db/demo/V1911__demo_renewal_candidates.sql` | R2 | Candidates at every stage on the booked demo invoices (`ARN-2026-940001..004`, Operations demo invoices). Dates relative to the load date are set by a Java demo runner `RenewalDemoData` (pattern of `BookingDemoData`), because the demo invoices expire about a year after booking |

Why this is safe:
- V1010+ runs after every V8xx table (including the shared V822) on a fresh database, so V1011 may alter `bkg_queue`, `quo_quotation` and `npk_proposal`. `rnw_*` still stores ARN and invoice no. as plain values, as Operations does.
- V1011 is additive (nullable columns; the check constraint is widened). Hibernate `validate` requires the entity changes in the same wave (R0).
- The demo V1910+ runs after the V9xx demo and the Collections demo (V1900-V1909).

## 4. Entities (key fields)

Every table has `company_id`, the audit columns of V1 and `version`. Money is `numeric(19,2)`.

### 4.1 Candidates and checks

- `rnw_extraction_run`: `run_no` (`RXR-<yyyy>`), trigger (SCHEDULED / MANUAL_RANGE / UPLOAD / LEGACY), expiry_from, expiry_to, requested_by, counts (read, new, existing, skipped), started / ended, status.
- `rnw_candidate`: one row per expiring **root invoice and policy year** (BRRN.005, 022).
  - Keys:
    - `renewal_ref` (`RNW-<yyyy>-nnnnnn`, unique per company);
    - `source` (BIBS_INVOICE / SUBMITTED_POLICY / LEGACY) and `source_ref` (SBM number or legacy reference), unique per company and source;
    - `expiring_invoice_no` (unique per company when present; empty for submitted and legacy policies);
    - `expiring_arn`, `expiring_policy_no`, `cover_no`, `version_no`, `pn_nos`.
  - Classification snapshot (listing and reports; refreshed at each evaluation):
    - client code and name, assured;
    - product, line, risk code, segment, `business_origin`, `account_type`;
    - invoicing branch, department code and name, sales unit, unit head, AO;
    - BDO branch / area / region, bank officer / unit;
    - insurer, insurer group, `mortgaged`, mortgagee bank, packaged flag;
    - inception, expiry;
    - basic / gross premium, total SI, premium rate, commission rate, currency.
  - Lifecycle:
    - `stage` (mirrors the `RNW_CASE` work case);
    - `bucket` CLEAN / REVIEW / EXCEPTION;
    - `bucket_rule_version`, `evaluated_at`;
    - `disposition`, `nonrenewal_reason`, `disposition_source`;
    - `initiated_by` / `initiated_at`;
    - `assigned_ao`, `assigned_po`, `owner_unit`;
    - `marketing_locked_at` (RA), `history_viewed` (BRRN.027).
  - Flags:
    - `stp` (matrix AUTO), `nrns`, `kyc_due` (+ `kyc_flagged_at`), `claims_flag`, `endorsement_pending`, `outstanding_flag`, `transferred`, `returned`;
    - `ra_notice` (NONE / FIRST / SECOND), `path` (STANDARD / NB_PATH / STP).
  - Links: `renewal_arn` (renewal account), `quotation_ref` / `proposal_ref` (NB path), `renewed_invoice_no` (booking result).
  - Closure: `closed_as` (RENEWED / NOT_RENEWED / LOST / EXPIRED_UNRENEWED / BOOKED_OTHER_INVOICE), `closed_at`.
  - Indexes: (company, stage), (company, expiry), (company, assigned_ao, stage), (company, assigned_po, stage), (company, bucket), (company, owner_unit), `expiring_arn`, `pn_nos` (gin trigram), and a search vector (names, refs).
- `rnw_check_run` (candidate, trigger EXTRACTION / UPLOAD / EVENT / NIGHTLY / MANUAL, at) and `rnw_check_result` (run, `check_code`, outcome PASS / WARN / FAIL / INFO / NOT_APPLICABLE, severity, detail JSON, message). This is the Checks & Bucket tab and RNW-SANITATION.
- `rnw_bucket_history`: from / to bucket, rule set version, rule id, check run, cause (RULE / OVERRIDE), user, remarks (BRRN.023 AC 6).
- `rnw_override`: kind (BUCKET / DISPOSITION / OUTSTANDING_BALANCE / INSURER_MISMATCH / RA_UNLOCK), from / to, remarks (mandatory), user, time (BRD 1.011, BRRN.023/031/035).
- `rnw_remark` (text <= 200, stage, user, time) and `rnw_followup` (Contact Center: channel, outcome LOV, remarks, next date; BRRN.026).
- `rnw_history_view` (candidate, user, time, sections viewed) for the BRRN.027 gate.

### 4.2 Rules and setup

- `rnw_non_renewable_risk_code`: risk code, line (nullable), reason, effective from / to; `AuthorizableEntity` (BRRN.009).
- `rnw_check_setting`: check code, active, severity (FAIL_EXCEPTION / FAIL_REVIEW / WARN / INFO), parameters JSON (for example tolerance, days); maker-checker.
- `rnw_bucket_rule_set` (version, status DRAFT / ACTIVE / RETIRED, effective from, approved by) and `rnw_bucket_rule` (priority, condition over check outcomes, result bucket). The default rule set: any FAIL_EXCEPTION gives EXCEPTION; any FAIL_REVIEW or WARN gives REVIEW; otherwise CLEAN. **A FAIL can never give CLEAN.** A database check and a service guard enforce this.
- `rnw_decision_matrix` (version, status, effective from, approved by) and `rnw_decision_rule`:
  - priority;
  - criteria: segment, line, product, mortgaged, bucket, claims condition (NONE / OPEN / PAID / TOTAL_LOSS), endorsement condition (NONE / POSTED_IN_TERM / PENDING), payment condition (PAID / OUTSTANDING / DP), days-to-expiry from / to;
  - outcome disposition, automation AUTO / MANUAL, letter hint (RA / NFR / NAL).

  Every system decision stores the matrix version and the rule id (BRRN.034 AC 3).

### 4.3 Disposition and transfers

- `rnw_disposition`: append-only.
  - Fields: candidate, code (enum `RenewalDisposition`: FOR_RENEWAL / NOT_FOR_RENEWAL / FOR_QUOTATION / FOR_PROPOSAL / LOST_BUSINESS), reason (LOV `RNW_NONRENEWAL_REASON`), remarks, source (USER / SYSTEM_CHECK / MATRIX / UPLOAD / INSURER / LAMD), matrix version and rule, `superseded_by`, user, time.
  - The candidate points to the current row.
- `rnw_transfer`: candidate, from unit, to unit, requested by, remarks, status REQUESTED / ACCEPTED / DECLINED, decided by, decision remarks (BRD 1.006 / 1.007 / 2.005).
- `rnw_candidate_endorsement`: candidate, endorsement / request no., source (BOOKING / ADJUSTMENT), status at link time (BRRN.032 AC 3).

### 4.4 Processing, insurer and LAMD

- `rnw_upload_scope`: the bulk job id, kind (DISPOSITION / INSURER_RESPONSE / LAMD / LEGACY), and the declared scope: expiry range, unit, and "complete file" flag for the 3.004.4 tag.
- `rnw_insurer_batch`: `RIB-<yyyy>`, insurer, expiry range, status DRAFT / SENT / PARTIALLY_RESPONDED / CLOSED, file (attachment), message id, sent_at, reply_due. `rnw_insurer_batch_line` holds the candidate and the 28-column snapshot (BRD 3.009.1.4).
- `rnw_insurer_response`: append-only.
  - Fields: candidate, batch, response (RENEW_AS_IS / REVISE / REJECT), insurer reference, revised premium / SI / rate / terms JSON, received date, source (UPLOAD / MANUAL), match outcome (MATCHED / REF_MISMATCH / POLICY_MISMATCH / AMBIGUOUS), `latest_valid`, `late` flag.
  - The latest valid response drives the case (BRRN.035).
- `rnw_lamd_report` (type PAID_OFF / RMU, period, file, bulk job) and `rnw_lamd_line` (PN, loan status, dates, matched candidate, routing, match outcome) (BRRN.029).

### 4.5 Letters and acceptance

- `rnw_letter`:
  - Types: RA (templates GENERIC / FFY), NAL, NFR, NRNS reminder, SFU (submitted mortgaged accounts), non-acceptance; channel EMAIL / PRINT (PRINT through `submitted`'s `MailHouseGateway` for submitted policies).
  - Storage: each RA is an attachment of document type **`RENEWAL_ADVICE`** linked to the renewal account, the client and the candidate (EBQ28); other letters use `RENEWAL_LETTER`.
  - Fields: number (`RA-<yyyy>`, `NAL-<yyyy>`, `NFR-<yyyy>`, `RNL-<yyyy>` for reminders, `NAC-<yyyy>` for non-acceptance), type, notice FIRST / SECOND (RA), candidate, template code and version, document (attachment), generated by / at.
  - Delivery: status GENERATED / QUEUED / SENT / FAILED / CANCELLED, message id, recipients, protection flag.
  - `min_notice_warning_confirmed_by` (RA less than 30 days before expiry).
- `rnw_letter_batch`: selection, count, job run, status (batch generation and batch send).
- `rnw_acceptance`: candidate, method (EMAIL / SIGNED_RA / PAYMENT), evidence (attachment id or payment gate evidence reference), recorded by (user or SYSTEM), time, `financial_impact_ack` (BRRN.038/040).

## 5. Accounting events and GL entries

Renewal posts **no** business event. The money effects of a renewal are:

| # | Trigger | Posted by | Event | Default entry (demo rules, BROKING_ARCHITECTURE §15) |
|---|---|---|---|---|
| 1 | Renewal account booked (policy issued, from the queue) | booking | `BROKER_BOOKING` (existing), with the invoice fact `businessType = RENEWAL` | As NB: Dr PR by component (client) / Cr DTIP (insurer); Dr commission receivable / Cr unrealized commission and deferred output VAT (`OPS_COMMISSION_REALIZATION`) |
| 2 | Service invoice on booking | booking `ServiceInvoiceService` | as NB | as NB |
| 3 | Payment of the renewal premium | cashiering (Operations) | `OPS_PAYMENT_APPLY` (existing) | as Operations |
| 4 | NB-path renewal (quotation / PRF) | booking | as 1 | as 1 |

Consequences:
- The accounting rule may condition on the business type. The booking event already carries the invoice facts, so Comptrollership can split premium or commission accounts for renewals without code if they want to (AQ01 / OQ07 decide).
- Operations (`opsledger` classification) and ACSL (spec BDOI_ACCT Appendix C, account status "Renewal") read the business type from `InvoiceBooked`. `opsledger` needs a column only if it is not already copied (section 13).
- No new GL accounts. The expiring invoice is not touched: a renewal is a new invoice, never an endorsement.

## 6. Security

### 6.1 Permissions (added to `security.domain.Permission` in R0, granted in V1010)

| Permission | Used for | BRD function (6.002.2.n) |
|---|---|---|
| `RNW_VIEW` | Renewal home, lists, record page, history | 4 view account, 11, 13, 17 |
| `RNW_EXTRACT` | Generate the expiring list (range), run the extraction, initiate candidates | 3, 15 |
| `RNW_ASSIGN` | Assign / re-assign AOs, transfer, receive transfers | 5, 6, 7 |
| `RNW_DISPOSE` | Provide disposition, remarks, update returned accounts, NB path | 12, 14 |
| `RNW_REVIEW` | TL review, return, post | 8, 22 |
| `RNW_OVERRIDE` | Outstanding-balance override, bucket / disposition override, insurer mismatch override, RA unlock | 1.011, BRRN.023/031/035 |
| `RNW_PROCESS_ASSIGN` | Assign accounts to Processing Officers | 5 (processing) |
| `RNW_PROCESS` | Update data, review computations, return to Marketing | 18, 19, 22 |
| `RNW_UPLOAD` | Upload dispositioned files | 16 |
| `RNW_INSURER` | Extract per insurer, send, upload insurer responses | 20 |
| `RNW_RA_GENERATE` | Generate / view / download RA, NAL, NFR | 10, 21 |
| `RNW_RA_SEND` | Send letters in batch | 10 |
| `RNW_ACCEPT` | Record client acceptance | BRRN.040 |
| `RNW_FOLLOWUP` | Contact Center remarks, documents and follow-ups | BRRN.026 |
| `RNW_LAMD_UPLOAD`, `RNW_VALIDATE` | LAMD reports and validation checks | BRRN.024/029 |
| `RNW_REPORT_VIEW`, `RNW_EXPORT` | Renewal reports, exports | 9 |
| `RNW_SETUP` | Non-renewable risk codes, check settings, bucket rules, decision matrix, lead days (activation needs `MASTER_AUTHORIZE`, never the maker) | 23 (LOVs via `LOV_MANAGE`) |
| `RNW_TEMPLATE_MAINTAIN` | Update the renewal templates | 25 |
| existing `ATTACHMENT_MANAGE`, `ACCESS_REQUEST` / `ACCESS_APPROVE`, `LOV_MANAGE` | Acquire documents (1), maintain users (24), maintain LOVs (23) | 1, 23, 24 |

Multi-tab (function 2) needs no permission; it is the platform behaviour for every user.

### 6.2 Roles (V1010) and demo users (V1910, password `Brokerverse@2026`)

| Role | Persona | Renewal permissions | Demo user |
|---|---|---|---|
| `MKT_TL` (exists) | Marketing Team Leader | VIEW, EXTRACT, ASSIGN, REVIEW, OVERRIDE, DISPOSE, RA_GENERATE, RA_SEND, ACCEPT, REPORT_VIEW, EXPORT | `mkttl` (exists), `rnwtl` (second TL, receiving unit for transfers) |
| `MKT_AO` (exists) | Marketing AO / Admin, Account Broker | VIEW, DISPOSE, ASSIGN (transfer request only; enforced in the service), RA_GENERATE, RA_SEND, ACCEPT, REPORT_VIEW, EXPORT | `ao`, `ao2` (exist) |
| `PROCESSING_TL` (exists) | Processing Team Leader | VIEW, EXTRACT, PROCESS_ASSIGN, PROCESS, UPLOAD, INSURER, RA_GENERATE, RA_SEND, ACCEPT, REPORT_VIEW, EXPORT | `proctl` (exists) |
| `PROCESSOR` (exists) | Processing Officer / Broker | VIEW, PROCESS, UPLOAD, INSURER, RA_GENERATE, RA_SEND, ACCEPT, REPORT_VIEW | `proc` (exists) |
| `BUSINESS_ADMIN` (exists) | Business Administrator | VIEW, SETUP, TEMPLATE_MAINTAIN, REPORT_VIEW (+ existing LOV_MANAGE, ACCESS_REQUEST) | `badmin` (exists) |
| `SYSADMIN` (exists) | System Administrator | role-permission change requests (existing) | existing |
| `LAMD` (new) | LAMD, validation only (BRRN.024) | VIEW, LAMD_UPLOAD, VALIDATE | `lamd` |
| `CONTACT_CENTER` (new) | Contact Center (BRRN.026) | VIEW, FOLLOWUP (+ ATTACHMENT_MANAGE on candidates only) | `contactc` |

A test asserts the BRRN.024 negative rule: `LAMD` has no `PLACEMENT_*`, `BOOKING_*`, `EPOLICY_*`, `RNW_RA_*`, `RNW_DISPOSE` or messaging permission. The final matrix is open (OQ48, RQ20, RQ21).

**Data scope.**
- Marketing users see the candidates of their sales units (`SalesOrganisationService.assignmentOf`), and AOs see the ones assigned to them.
- Processing sees the processing stages.
- Contact Center and LAMD see a read-only projection without premium columns (to confirm, RQ21).
- The scope is applied in the query service, never in the UI only.

## 7. Workflows

### 7.1 `RNW_CASE` (seeded in V1010)

```
EXTRACTED --initiate(RNW_EXTRACT)--> EVALUATING --(system: checks + bucket + matrix)-->
   UNASSIGNED            (non-CBG, MANUAL or not CLEAN)           [tab "Unassigned Disposition"]
   FOR_TL_REVIEW         (CLEAN + AUTO: system disposition, TL review skipped when RNW_STP_SKIP_TL_REVIEW)
   FOR_PROCESSING        (CBG STP, BRRN.039)
   LETTER_PENDING        (system NOT_FOR_RENEWAL: non-renewable risk code, LAMD paid-off / RMU)

UNASSIGNED --assign(RNW_ASSIGN)--> FOR_DISPOSITION --dispose+push(RNW_DISPOSE)--> FOR_TL_REVIEW ("Review in progress")
UNASSIGNED/FOR_DISPOSITION --transfer_request--> TRANSFER_PENDING --accept(receiving TL)--> UNASSIGNED (other unit)
                                                                 --decline(remarks)--> previous stage
FOR_TL_REVIEW --return(reason)--> FOR_DISPOSITION (flag RETURNED)
FOR_TL_REVIEW --post(RNW_REVIEW)--> by disposition:
    FOR_RENEWAL        -> FOR_PROCESSING
    FOR_QUOTATION / FOR_PROPOSAL -> NB_PATH
    NOT_FOR_RENEWAL    -> LETTER_PENDING
    LOST_BUSINESS      -> CLOSED
FOR_PROCESSING --assign_po(RNW_PROCESS_ASSIGN)--> IN_PROCESSING --return(RNW_PROCESS)--> FOR_DISPOSITION | FOR_TL_REVIEW
IN_PROCESSING --send_to_insurer(RNW_INSURER)--> WITH_INSURER
WITH_INSURER --insurer_response(system/RNW_INSURER):
    RENEW_AS_IS + matched                   -> RA_READY
    REVISE                                  -> IN_PROCESSING (financial-impact check; NB_PATH if the matrix says so)
    REJECT                                  -> FOR_DISPOSITION (re-market) | LETTER_PENDING
    mismatch / late / conflicting           -> stays, bucket EXCEPTION
RA_READY --generate_ra(RNW_RA_GENERATE)--> RA_GENERATED (Marketing lock) --send(RNW_RA_SEND)--> RA_SENT ("Awaiting Response")
RA_SENT --accept(RNW_ACCEPT | system: payment)--> ACCEPTED --(system: renewal account fast-track)--> FOR_PLACEMENT_BOOKING
FOR_PLACEMENT_BOOKING --(system: InvoiceBooked of renewal ARN)--> RENEWED [terminal]
NB_PATH --(system: quotation/PRF accounts booked)--> RENEWED | --(system: declined / voided)--> CLOSED(LOST)
LETTER_PENDING --send_nfr/nal--> CLOSED(NOT_RENEWED) [terminal]
RA_SENT / FOR_DISPOSITION / UNASSIGNED --(system: expiry + RNW_NON_ACCEPTANCE_DAYS, NRNS)--> CLOSED(EXPIRED_UNRENEWED)
CLOSED(NOT_RENEWED) --reopen(RNW_DISPOSE, until expiry + RNW_REOPEN_DAYS)--> FOR_DISPOSITION      (BRD 2.004.10 / 3.004.5)
any open stage --override(RNW_OVERRIDE, remarks)--> (target of the override)
```

Rules:
- Stage SLAs are measured against the **expiry date**, not the stage age.
- `WorkSlaAlertCheck` raises `RNW_RENEWAL_AT_RISK` from `RNW_ESCALATION_DAYS` (BRRN.036).
- **Marketing lock.** From RA_GENERATED, disposition and editing return `RENEWAL_LOCKED` (BRD 2.004.9). Only `RNW_OVERRIDE` with RA cancellation unlocks.
- **Blocking checks.** `post`, `generate_ra` and `accept` are refused while any FAIL check is open, unless it is overridden (BRRN.023/031/032/035).
- **History gate.** `dispose` is refused with `HISTORY_NOT_VIEWED` until the user opened Account History (BRRN.027 AC 4).

### 7.2 `NB_ACCOUNT` addition (V1011)

`DRAFT --renewal_fast_track (system)--> AWAITING_PAYMENT`:
- It is allowed only for `business_type = RENEWAL`.
- It is called by `AccountService.fastTrackRenewal(arn)`, which runs the same completeness checks as `submit` and `validate`: mandatory fields, required documents, rated premium, confirmed client, TSU clearance. It fails with the same error codes.
- The payment gate, placement, issuance and booking then follow the NB workflow unchanged (BRRN.040 AC 5 "standard SLAs").

### 7.3 Candidate lock and duplicate rules

- One open candidate per expiring invoice.
- A candidate whose renewal account is booked closes RENEWED. The next extraction then finds the **renewal** invoice as the new expiring policy of next year.
- Multi-year accounts (BRNB.108) produce a candidate only for the last policy year of the term.

## 8. Rules engine

### 8.1 Checks (`renewal.service.check`, one `RenewalCheck` bean each)

| Code | BR | Source | Default severity |
|---|---|---|---|
| `REFERENCE_MATCH` | BRRN.022 | Candidate vs expiring invoice / ARN / policy; uploads carry the ref | FAIL_EXCEPTION |
| `PN_PRESENT` | BRRN.029/031/039 | PN on the account or invoice, required for mortgaged and CBG lines | FAIL_EXCEPTION |
| `RISK_CODE_RENEWABLE` | BRRN.009 | `rnw_non_renewable_risk_code` | system NOT_FOR_RENEWAL (no bucket effect) |
| `LAMD_STATUS` | BRRN.029/039 | `rnw_lamd_line` matched by PN | system NOT_FOR_RENEWAL / route to RMU |
| `CLAIMS` | BRRN.031/034 | `brokerclaims.service.ClaimExperienceQueryService.summary(arn, policyYear)` (bean absent: INFO "claims not connected") | FAIL_REVIEW when open claims; total loss parked (CLQ28) |
| `ENDORSEMENT_PENDING` | BRRN.032 | `AdjustmentQueryService.forInvoice` over the invoice family, open requests | FAIL_REVIEW |
| `OUTSTANDING_PREMIUM` | BRD 1.011, BRRN.031 | `InvoiceLedgerQueryService` family balance > `RNW_OUTSTANDING_THRESHOLD` | FAIL_REVIEW (TL override) |
| `FINANCIAL_IMPACT` | BRRN.038 | Renewal account or insurer revision vs expiring premium / SI / rate / charges | FAIL_REVIEW, routes to the NB path by the matrix |
| `INSURER_RESPONSE_MATCH` | BRRN.035 | Latest valid response matched, not late or conflicting | FAIL_EXCEPTION |
| `MANDATORY_FIELDS` | BRD 2.004.5, 3.007.3 | Disposition requirements and the catalog field matrix | FAIL_REVIEW |
| `PRODUCT_RENEWABLE` | PQ11 | `ProductCatalogService.requireSellable(code, RENEWAL, date)` / `SchemeResolver` | FAIL_REVIEW (RQ30) |
| `INSURER_USABLE` | - | `InsurerService.requireUsableInsurer` | FAIL_EXCEPTION |
| `DUPLICATE_CANDIDATE` | BRRN.005 | Another open candidate or live renewal account on the same risk (`RiskDuplicateService`) | FAIL_EXCEPTION |
| `KYC_DUE` | BRRN.028 | `Client.kycReviewDue` within `KYC_DUE_WINDOW_DAYS`, scope `RNW_KYC_SEGMENTS` | INFO only, never blocks |

**Re-evaluation.** The checks run again on these events:
- `InvoiceBooked` and `InvoiceMovementPosted` of the family;
- `AccountStatusChanged`;
- every upload (disposition, insurer, LAMD);
- the nightly job `RNW_REEVALUATE`.

A new result set drives the bucket, so records move between buckets as conditions change (BRRN.023 AC 3).

### 8.2 Buckets and matrix

- The bucket is computed from the active `rnw_bucket_rule_set`. It is stored with the version, and every change is logged. The UI filters and exports by bucket, and the pill uses green / yellow / red (BDO_UX_GUIDELINES §4).
- The decision matrix is evaluated **after initiation** on CLEAN and REVIEW candidates:
  - The first rule by priority gives a proposed disposition and AUTO / MANUAL.
  - AUTO on a CLEAN candidate writes a system disposition (source MATRIX) and moves the case on (§7.1).
  - Otherwise the proposal is shown to the AO as the default.
- Neither the matrix nor the bucket rules ship with production content; demo content is in V1910 (RQ01, RQ24).

## 9. Jobs, parameters, LOVs, alerts, templates, numbers, bulk handlers

| ManagedJob | Default cron (UTC; PHT = UTC+8) | Purpose | BR |
|---|---|---|---|
| `RNW_EXTRACTION` | `0 0 17 * * *` (01:00 PHT) | Extract candidates whose expiry = business date + lead days (per segment); run the checks | BRRN.030/020 |
| `RNW_REEVALUATE` | `0 30 17 * * *` | Re-run the checks of open candidates changed since the last run or within 30 days of expiry | BRRN.023 |
| `RNW_NRNS_LETTERS` | `0 0 22 * * *` (06:00 PHT) | Flag NRNS; queue reminder letters at `RNW_NRNS_REMINDER_DAYS`; non-acceptance letters; second-notice RA candidates | BRRN.025/037, BRD 1.010.2.2 |
| `RNW_EXPIRY_SWEEP` | `0 15 16 * * *` | Close EXPIRED_UNRENEWED candidates past expiry + `RNW_NON_ACCEPTANCE_DAYS`; notify the owners | BRRN.037 |
| `RNW_LETTER_BATCH` | `-` (manual, run by the screen through `JobRunService`) | Generate and send letter batches (RA, NAL, NFR) with progress | BRRN.010 |

The crons are `brokerverse.jobs.renewal-*-cron` in `application.yml`, documented in `docs/operations/CONFIGURATION.md` (R0).

Parameters (`sys_parameter`, category RENEWAL):

| Parameter | Default |
|---|---|
| `RNW_EXTRACTION_LEAD_DAYS` | 140 |
| `RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT` | empty (CODE_LIST `segment=days`) |
| `RNW_BULK_INITIATION_SEGMENTS` | `CLG` |
| `RNW_CBG_STP_LINES` | `MOTOR,PROPERTY` for segment CBG |
| `RNW_STP_SKIP_TL_REVIEW` | true |
| `RNW_OUTSTANDING_THRESHOLD` | 0.00 |
| `RNW_FIN_IMPACT_TOLERANCE` | 0.00 |
| `RNW_RA_MIN_NOTICE_DAYS` | 30 |
| `RNW_RA_SECOND_NOTICE_DAYS` | 15 |
| `RNW_NRNS_REMINDER_DAYS` | 90 |
| `RNW_NON_ACCEPTANCE_DAYS` | 0 (at expiry) |
| `RNW_REOPEN_DAYS` | 30 |
| `RNW_ESCALATION_DAYS` | `IBG=60,LEASING=60,*=30` |
| `RNW_KYC_SEGMENTS` | empty = all |
| `RNW_RMU_UNIT` | empty |
| `RNW_AUTO_PLACEMENT` | true |
| `RNW_REFERENCE_PREFIX` | `RNW-<yyyy>` |
| `RNW_EXCLUDED_LINES` | EB benefit lines (HMO, GLI, GPA; EBQ28) |

All values are to be confirmed (RQ04, 09, 11, 18, 25).

LOV types (V1010, business-maintainable, BRD 5.003):
- `RNW_DISPOSITION` (labels only; codes fixed);
- `RNW_NONRENEWAL_REASON`, with the ten values of 2.004.4 and attributes `nal_eligible` and `requires_invoice_no`;
- `RNW_RETURN_REASON`;
- `RNW_TRANSFER_REASON`;
- `RNW_FOLLOWUP_OUTCOME`;
- `RNW_OVERRIDE_REASON`;
- `RNW_INSURER_RESPONSE`;
- `RNW_LAMD_STATUS`.

Alerts (`alt_exception_code`):
- `RNW_RENEWAL_AT_RISK` (days to expiry, not disposed or not accepted);
- `RNW_EXTRACTION_FAILED`;
- `RNW_INSURER_OVERDUE` (batch past `reply_due`);
- `RNW_LETTER_FAILED`;
- `RNW_EXCEPTION_AGEING`.

Notification events:
- `RNW_ASSIGNED`, `RNW_TRANSFER_REQUESTED`, `RNW_TRANSFER_DECIDED`, `RNW_RETURNED`, `RNW_POSTED`, `RNW_INSURER_RESPONDED`, `RNW_ACCEPTED`, `RNW_RENEWED`.

Numbers (`DocumentNumberService`): `RNW-<yyyy>` (renewal ref), `RXR-<yyyy>` (extraction run), `RIB-<yyyy>` (insurer batch), `RA-<yyyy>`, `NAL-<yyyy>`, `NFR-<yyyy>`, `RNL-<yyyy>`, `NAC-<yyyy>`.

Templates (`doc_template`, drafts until RQ26):
- `RNW_RA_FIRST`, `RNW_RA_SECOND` (policy details, renewal terms, premium, BDOI contacts; BRRN.010 AC 10);
- `RNW_RA_FFY` (FFY variant for submitted policies), `RNW_SFU`;
- `RNW_NAL`, `RNW_NFR`, `RNW_NRNS_REMINDER`, `RNW_NON_ACCEPTANCE`;
- `RNW_INSURER_COVER` (e-mail to the insurer);
- `RNW_ACCOUNT_DETAILS` (PDF of BRD 1.004.1.3).

Bulk handlers (`BulkImportHandler`):
- `RNW_DISPOSITION_UPLOAD` (`RNW_UPLOAD`; BRRN.018, BRD 3.004);
- `RNW_INSURER_RESPONSE` (`RNW_INSURER`; BRD 3.009.6, BRRN.035);
- `RNW_LAMD_REPORT` (`RNW_LAMD_UPLOAD`; BRRN.029);
- `RNW_LEGACY_POLICIES` (`RNW_EXTRACT`; RQ27);
- `RNW_ACCEPTANCE` (`RNW_ACCEPT`; bulk acceptance by e-mail list).

Each handler validates the renewal ref (BRRN.022). Unmatched rows are kept as EXCEPTION lines, never dropped.

## 10. Integrations to park (seam only)

| Item | BR | Seam built now | Question |
|---|---|---|---|
| Total-loss indicator | BRD 2.004.4.5, BRRN.034 | Manual non-renewal reason; matrix condition TOTAL_LOSS inactive | CLQ28, RQ13 |
| LAMD channel (reports from the bank) | BRRN.029/039 | `RNW_LAMD_REPORT` upload; the LAMD role can upload | RQ20 |
| Insurer channels (renewal files and responses) | BRD 3.009.5/6 | E-mail out (protected), upload in; `InsurerFileInbox` pattern (opsledger) not used until insurer SFTP / API exists | Q06, RQ15 |
| Legacy EBIX / QPS expiring policies at go-live | p.34, BRRN.005 | `RNW_LEGACY_POLICIES` bulk handler creates candidates with `legacy_ref` and no ARN; they can only take the NB path (no account to renew from) | RQ27 |
| Mail recipient policy (approved domains, TLS) | BRRN.010 negative 6 | `RecipientPolicy` port in `renewal` with a permissive default; TLS is the SMTP relay configuration | RQ16 |
| Password convention | BRRN.010, 3.009.5 | `DocumentPasswordPolicy` (messaging), generated passwords | Q07 |
| BDO CIF KYC data | BRRN.028 | BIBS KYC review date only | Q16, RQ22 |
| BDO SSO | BRD x.001 | Platform JWT login | Q42 |

## 11. Reports (`ReportDefinition`, category "Renewal", package `renewal.report`)

| Code | Class | Content | BR |
|---|---|---|---|
| `RNW-EXPIRY-LIST` | `ExpiryListReport` | The grid's rows and columns (spec 6.1) with the filter state | BRRN.004/007/008 |
| `RNW-STATUS` | `RenewalStatusReport` | Per invoice, 37 / 40 columns (variant MARKETING / PROCESSING), 34-line summary | 1.009 / 2.008 / 3.010 / 4.008, BRRN.019 |
| `RNW-LISTING` | `RenewalListingReport` | Unit / segment / window / status; ageing to expiry; escalation flag | BRRN.036 |
| `RNW-INSURER-EXTRACT` | `InsurerExtractReport` | 28 columns per insurer | 3.009, 4.007 |
| `RNW-RA-DISPATCH` | `LetterDispatchReport` | RA / NAL / NFR / NRNS letters, recipients, status | 1.010.6, BRRN.001/010/025/037 |
| `RNW-SANITATION` | `SanitationReport` | Check results, bucket and changes | BRRN.020/023 |
| `RNW-DECISIONS` | `DecisionLogReport` | Dispositions, matrix rule and version, overrides with rationale | BRRN.031/034, 1.011 |
| `RNW-LAMD-MATCH` | `LamdMatchReport` | LAMD lines matched / unmatched, routing | BRRN.029 |
| `RNW-WORKLOAD` | `WorkloadReport` | Accounts per AO / PO by stage | 1.009.7, 3.010.7 |

Every report reads through constant SQL (the `NbReportJdbc` pattern) with named parameters, the criteria of spec 6.2 as `CODE_SET` parameters, and role scope. The target is under 20 s at 30,000 rows.

**Summary counter mapping (proposal, RQ10).** Each counter is `count(*) filter (where …)` over the candidates in scope:
- Booked = closed RENEWED; Cancelled = the renewal account CANCELLED;
- For Placement = FOR_PLACEMENT_BOOKING; For Proposal = NB_PATH with a PRF; For ARF = NB_PATH with a quotation, pending the definition of ARF;
- RA Processed = RA_SENT or later; For RA Processing = RA_READY or RA_GENERATED; Renew to TSU = NB_PATH routed to TSU (`TsuRoutingService`);
- Returned to Marketing = returned flag; Awaiting TL Approval (x4) = FOR_TL_REVIEW by disposition; Disapproved = TL return with reason `DISAPPROVED`;
- Not For Renewal = the disposition;
- the mortgaged / non-mortgaged pairs split by `mortgaged` and by reason: Lost Business, For Proposal, Transferred, Loan Fully Paid, Awaiting Confirmation (RA_SENT), Booked under New Invoice (reason `BOOKED_TO_NEW_INVOICE`), No Disposition (UNASSIGNED / FOR_DISPOSITION), to Other Bank (reason to add, RQ10);
- Non-Renewable = reason `NON_RENEWABLE_ACCOUNT`; No Disposition = no disposition; Total = all.

## 12. Screens (group **Client & Policy**, section **Renewal**, after Placement & Booking)

The module is `features/renewal/module.ts`, route `/renewal`, listed in `navigation/modules.ts` in the `client-policy` group after `bookingModule`, where the UX guideline reserves "Renewal (later BRD)". Screens follow `docs/design/BDO_UX_GUIDELINES.md`: tokens only, the §5 work-list pattern, `WorklistToolbar`, `Tabs` on a `Card flush`, `StatusBadge` for the pill, `.tag` flag chips and `RecordSummary`. Each screen has a help entry in `features/renewal/help.ts`.

| Screen | Route | Permission | Content |
|---|---|---|---|
| Renewal Home | `/renewal` | RNW_VIEW | Tiles by stage and bucket, expiring in 30 / 60 / 90 / 140 days, at risk, exceptions, insurer overdue, letters failed; workload per AO / PO (1.009.7, 3.010.7) |
| Expiry List (prototype "Renewal") | `/renewal/expiry` | RNW_VIEW | Tabs **Unassigned Disposition** / For Renewal / For Quotation / For Proposal / Not for Renewal / Lost Business / Exceptions / All. Toolbar: "Search Renewal Ref / Proposal No.", Filters (`MultiSelectFilter`), bulk actions **Generate Expiry List** (dialog with a date range), **Initiate**, **Assign Disposition**, **Re-assign Officer**, **Transfer**. `GridTable` with a Classification pill (Clean / Review / Exception) and flag chips |
| My Dispositions | `/renewal/mine` | RNW_DISPOSE | AO list, all statuses and years (BRRN.011); quick filters Returned to me, Due in 30 days, NRNS |
| TL Review | `/renewal/review` | RNW_REVIEW | "Review in progress" list; bulk Return / Post; Override outstanding balance (1.011) |
| Transfers | `/renewal/transfers` | RNW_ASSIGN | Incoming / outgoing requests; accept / decline with remarks |
| Processing Worklist | `/renewal/processing` | RNW_PROCESS | Tabs For Processing / In Processing / With Insurer / Insurer Responded / Returned; Assign PO / Assign to Me; Upload Dispositioned File |
| Insurer Batches | `/renewal/insurer` | RNW_INSURER | Build per insurer and range, preview the 28 columns, download xlsx, send protected, upload responses, match review |
| Letters | `/renewal/letters` | RNW_RA_GENERATE | Tabs RA Ready / RA Generated / RA Sent / NAL / NFR / NRNS; bulk Generate (first / second notice), Preview, Download (ZIP), Send in Batch; 30-day warning dialog |
| Acceptance | on the record page and in bulk `/bulk/RNW_ACCEPTANCE` | RNW_ACCEPT | Record the method and evidence |
| Follow-ups | `/renewal/followups` | RNW_FOLLOWUP | Contact Center list (read-only projection), remarks, documents |
| LAMD Reports | `/renewal/lamd` | RNW_LAMD_UPLOAD | Upload, match results, routing |
| Record page | `/renewal/candidates/:ref` | RNW_VIEW | Back arrow, `RecordSummary` (ref chip, bucket pill, disposition, flags), `WorkflowPanel`, tabs Details / Checks & Bucket / Account History / Computations / Insurer / Letters / Documents / Remarks & Follow-ups / History |
| Renewal Reports | `/renewal/reports` | RNW_REPORT_VIEW | The section 11 reports through the Report Centre runner |
| Renewal Setup | `/renewal/setup` | RNW_SETUP | Non-renewable risk codes, check settings, bucket rule sets, decision matrix (versions, activation by a checker), parameters; links to the LOV maintenance and to the templates |

The crm client page gains a **Renewal** tab (prototype "Client Record Details": Quotation / Confirmed Proposals / **Renewal**) through `RenewalClientRecords implements ClientRecordsProvider`.

## 13. Impact on built and in-progress modules (contract changes)

| Module (state) | Change | Concrete contract | Owner / wave |
|---|---|---|---|
| `account` (built) | Business type and renewal link: **the shared change** | `V822__account_business_type.sql` and its code (account owner, designed in SUBMITTED_POLICIES_DESIGN): `business_type`, `renewal_of_ref`, `NewAccount.renewal(...)`, `Account.getBusinessType()`, `AccountSearch` filter. Renewal asks two additions to it: `NewAccount.renewal(..., Integer productVersionNo)` and the rating row below. No Renewal migration touches `acc_account` | account owner (before R0) |
| `account` (built) | Rating purpose (addition to the shared change) | `AccountPricing` passes `RatingQuery.Purpose.RENEWAL` and the given `productVersionNo` when `businessType = RENEWAL`, otherwise NEW_BUSINESS as today (closes the seam of PRODUCT_MAINTENANCE_DESIGN §4 "RENEWAL is the seam") | R0 |
| `account` (built) | Fast track | `AccountService.fastTrackRenewal(String arn)`: system transition `renewal_fast_track` (V1011) after the submit and validate checks. Renewal builds the `NewAccount.renewal(...)` request from the expiring account (client, product, items, insurer, PN / mortgage, contact, sales stamp; next period from = old to + 1 day, same term; `renewal_of_ref` = expiring ARN) | R0 |
| `booking` (built) | Business type from the account | `InvoiceBuilder` line 129 reads `account.getBusinessType()` instead of `BusinessType.NEW_BUSINESS`: part of the **shared** V822 change (account / booking owners). Renewal adds only `QueueSource.RENEWAL` (+ V1011 check constraint on `bkg_queue.source`) | shared change; R0 for the queue source |
| `quotation`, `nonpackage` (built) | NB path | `QuotationService.create` / proposal create accept an optional `renewalRef` (stored in `renewal_ref`, V1011); `create_accounts` then builds the accounts with business type RENEWAL and `renewal_of_ref` = expiring ARN through the shared `NewAccount` contract. `QuotationQueryService.getByRenewalRef` for Renewal status tracking | R0 (contract) / R1-B (use) |
| `placement` (built) | None in code | Renewal calls `HoldCoverService.request` for submitted-policy renewals (30 days; the re-assignment change is Submitted Policies'), and the public `PlacementSlipService.generate(companyId, arns)` / `send(slipId, draft(slipId))` on READY_FOR_PLACEMENT when `RNW_AUTO_PLACEMENT` is on. The payment gate is unchanged (BRRN.040 "payment rules satisfied") | - |
| `issuance` (built) | None | The renewal policy / e-policy is received as for NB (RQ19) | - |
| `catalog` / `productmaint` (built, PM) | None | `RatingQuery.Purpose.RENEWAL`, `SchemeResolver.renewal`, `ProductCatalogService.requireSellable` are used as designed (PQ11) | - |
| `opsledger` (built) | Read only; business type on the ledger | Renewal reads `InvoiceLedgerQueryService.search / family / movements` and `Invoice360Service`. `ops_invoice` has no business type today (checked in `opsledger/domain/OpsInvoice.java`). `InvoiceLedgerFeed` should copy it from `InvoiceBooked` into a new `ops_invoice.business_type` column (Operations range, `opsledger` owner). ACSL needs it for "account status Renewal"; Renewal does not need it for extraction, because every root invoice is a candidate | Operations owner (requested) |
| `adjustment` (built) | None | Read `AdjustmentQueryService.forInvoice` for the ENDORSEMENT_PENDING check | - |
| `collections`, `cashiering`, `commission`, `remittance` (Operations / BRD-4) | None | Outstanding balance is read from `opsledger`. The special remittance condition "renewal" (MKTID.009) stays a value | - |
| `disbursement`, `payrequest`, `acsl`, `frbs` (BRD-5) | None | ACSL reads the business type from booking (Appendix C "Renewal") | - |
| `crm` (built) | Client 360 tab | `RenewalClientRecords implements ClientRecordsProvider` (in `renewal`); KYC read through `ClientService` / `Client` getters | R1-B |
| `nbadmin` (built) | None | Role-permission change requests assign the `RNW_*` functions (BRD 6.002.2); retention: `RenewalRetentionProvider implements RetentionCandidateProvider` (record type RENEWAL_CANDIDATE, closed stages) | R2 |
| `report` (platform, built) | Parameter kind | `ParameterType.CODE_SET` and `ParameterSpec.codeSet(name, label, lovType)` giving the value `CodeSet(values, exclude)`; the runner UI uses `MultiSelectFilter`; `ReportCategory.RENEWAL` | R0 |
| `security` (platform) | Permissions and roles | `RNW_*` in `Permission.java`; roles LAMD, CONTACT_CENTER (V1010) | R0 |
| `frontend/src/components` (shared) | New components | `ui/GridTable.tsx` (sort, column filter, resize, reorder, virtual scroll, highlight, sticky header), `broking/MultiSelectFilter.tsx`; `DataTable` unchanged | R0 |
| `submitted` (Submitted Policies, planned) | Renewal implements `RenewalHandOff` | `SubmittedPolicyRenewalHandOff` in `renewal`; the default `SubmittedRenewalHandOff` steps aside (`@ConditionalOnMissingBean`); letters for submitted renewals are `rnw_letter` rows, printed through `MailHouseGateway`; SQ10 answered as "Renewal owns it" | R3 (after S1-D) |
| `brokerclaims` (Claims, planned) | None | Renewal calls `ClaimExperienceQueryService.summary(arn, policyYear)`; total-loss indicator is CLQ28 | - |
| `eb` (Employee Benefits, planned) | None | EB lines excluded from `RNW_EXTRACTION` (`RNW_EXCLUDED_LINES`); RAs stored as `RENEWAL_ADVICE` (EBQ28). An EB query "ARNs of EB cycles" would sharpen the exclusion (optional) | - |
| `nbreport` (built) | None now | BRNB.018 Late Renewal Requests stays parked (Q09, RQ29); candidate later as a `RNW-LISTING` variant | - |

## 14. Build-wave plan

Prerequisites: the BRD-1 modules and Product Maintenance P1-A (RENEWAL purpose) are built, and so is `opsledger`. Collections (C1) and Accounting (A1) run in parallel and do not touch any file below.

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **R0** (1 agent) | Renewal foundation and contracts | Prerequisite: the shared account / booking business-type change V822 is merged (account owner). Permissions and roles; V1010 / V1011; the fast-track transition, the RENEWAL rating addition, `QueueSource.RENEWAL`, the quotation and nonpackage `renewalRef` (section 13); `report` `CODE_SET`; `GridTable`, `MultiSelectFilter`; `renewal` package skeleton (package-info and `renewal.service.port` interfaces only; the domain belongs to R1-A); `features/renewal/module.ts` with a stub Home and `help.ts`; crons in `application.yml`; demo V1910 | `security/domain/Permission.java`; `account/service/AccountService.java` (`fastTrackRenewal`), `account/service/AccountPricing.java` (only if the shared change did not include the RENEWAL purpose); `booking/domain/QueueSource.java`; `quotation/service/QuotationService.java` + DTO; `nonpackage/service/*Proposal*Service.java` + DTO; `report/core/ParameterSpec*`; `frontend/src/components/ui/GridTable.tsx`, `frontend/src/components/broking/MultiSelectFilter.tsx`; `navigation/modules.ts`, `features/help/helpContent.ts`; `db/migration/V1010`, `V1011`; `db/demo/V1910`; `application.yml`, `docs/operations/CONFIGURATION.md` | Existing NB tests green; a RENEWAL account books with business type RENEWAL; `mvn verify`, `npm run verify` green |
| **R1-A** | Candidates, checks, buckets, matrix | Extraction job and on-demand extraction; `RenewalCheckEngine` and the section 8 checks; bucket rules; decision matrix; initiation; re-evaluation listeners; non-renewable risk codes; Setup screens; Expiry List grid; record page tabs Details / Checks / History; RNW-EXPIRY-LIST, RNW-SANITATION | `renewal/{candidate,extraction,check,rules,setup}/**`, `renewal/report/{ExpiryList,Sanitation}Report.java`, `V1012`, `V1013`, `features/renewal/{expiry,setup,record}/**` | Demo extraction creates candidates; a FAIL never gives CLEAN; the matrix AUTO path disposes a clean candidate |
| **R1-B** | Marketing: assignment, transfer, disposition, review, NB path | Assignment / re-assignment, transfers, disposition with reasons and history gate, remarks, TL review / return / post, overrides, NB path (quotation / PRF creation with `renewalRef`), client 360 tab, My Dispositions, TL Review, Transfers screens, RNW-DECISIONS | `renewal/{assignment,transfer,disposition,review,nbpath,client}/**`, `renewal/report/DecisionLogReport.java`, `V1014`, `features/renewal/{mine,review,transfers}/**` | Assign -> dispose -> return -> post; NB path creates a quotation whose account is RENEWAL |
| **R1-C** | Processing, insurer, LAMD | Processing worklist and PO assignment; renewal account creation (`AccountService.createDraft(NewAccount.renewal(...))`) at FOR_PROCESSING; computations tab; dispositioned-file upload with scope (3.004.4); insurer batches (xlsx, protected send) and response upload with matching and progression; LAMD upload and routing; Insurer / LAMD screens; RNW-INSURER-EXTRACT, RNW-LAMD-MATCH | `renewal/{processing,insurer,lamd,upload}/**`, `renewal/report/{InsurerExtract,LamdMatch}Report.java`, `V1015`, `features/renewal/{processing,insurer,lamd}/**` | Insurer Renew As Is moves to RA_READY; mismatch -> EXCEPTION; paid-off PN -> Not for Renewal |
| **R1-D** | Letters, acceptance, progression | RA / NAL / NFR / NRNS / non-acceptance letters, batch generation and protected send, RA lock and second notice, NRNS and expiry jobs, acceptance (single and bulk), `RenewalProgression` (fast-track, auto placement, booking queue, RENEWED on `InvoiceBooked`), Letters screen, RNW-RA-DISPATCH | `renewal/{letter,acceptance,progression,followup}/**`, `renewal/report/LetterDispatchReport.java`, `V1016`, `features/renewal/{letters,followups}/**` | RA sent -> accepted -> slip sent -> policy issued -> booked RENEWAL -> candidate RENEWED |
| **R3** | Submitted-policy hand-off (after Submitted Policies S1-D) | `SubmittedPolicyRenewalHandOff` implementing `submitted.service.port.RenewalHandOff`; FFY RA and SFU templates; print channel through `MailHouseGateway`; E2E masterlist record -> candidate -> renewal account -> hold cover -> RA -> booked -> masterlist BOOKED | `renewal/submitted/**`, template rows in a new V1017 | The default hand-off steps aside; no second RA for the same client |
| **R2** | Reports, home, hardening | RNW-STATUS (variants, 34 counters), RNW-LISTING with the escalation alert, RNW-WORKLOAD, Renewal Home, retention provider, demo V1911 + `RenewalDemoData`, E2E tests, module guide `docs/modules/RENEWAL.md`, traceability | `renewal/{home,report/RenewalStatusReport,…Listing,…Workload,retention,demo}/**`, `db/demo/V1911`, `features/renewal/{home,reports}/**`, `docs/modules/RENEWAL.md` | All reports export in under 20 s on demo volume; `mvn verify` / `npm run verify` green |

Rules for parallel work:
- R0 is merged before R1-A..D start. R1-A..D run in parallel. R2 starts when R1-A and R1-D are merged.
- One Flyway file per wave: R0 V1010 / V1011 / V1910; R1-A V1012 / V1013; R1-B V1014; R1-C V1015; R1-D V1016; R2 V1911; R3 V1017. Cross-table foreign keys between R1 migrations are forbidden: reference `rnw_candidate(id)` only, which V1012 creates. V1014-V1016 have a higher version than V1012, so they run after it.
- `renewal/candidate` (entity, repository, `CandidateQueryService`, `CandidateStageListener`) and `renewal/common` (audit helper, scope filter) are owned by R1-A. R1-B..D use them read-only and ask R1-A for changes. R1-A publishes the stubs in its first commit.
- `RNW_CASE` transitions are all seeded in V1010 (R0). The R1 agents only implement the endpoints of their actions.
- Shared files (the Permission enum, nav, help registry, `application.yml`, `components/ui`, the account / booking / quotation / nonpackage contracts) are edited **only in R0**.
- Each agent writes its own `*ApiIT` tests. `ApiSmokeIT` is a shared file: R0 adds the renewal read endpoints listed in section 12 as they are merged, and each R1 agent sends its endpoint list to the R2 agent, who adds the rest.

## 15. What depends on information BDOI has not given (build the seam, park the content)

| Item | Question | Built now |
|---|---|---|
| Check list and severities; bucket rules; decision matrix content | RQ01, RQ24 | Engine, setup screens, demo content only |
| Unique reference semantics and file keys | RQ03 | `RNW-<yyyy>` generated; expiring invoice no. as the alternate key |
| Summary counters (ARF, TSU, Disapproved, Other Bank) | RQ10 | Mapping of section 11; unknown ones return 0 with a "pending definition" note |
| Letter layouts | RQ26 | Draft templates (versioned, updatable by the Business Admin) |
| Insurer and LAMD file layouts, channels | RQ15, RQ20 | Upload handlers with configurable column mapping |
| NFRs "follow QPS" | RQ28 | BRD-1 NFR set |
| Legacy expiring policies | RQ27 | Legacy upload handler, NB path only |
| Total-loss indicator | CLQ28, RQ13 | Manual reason; matrix condition parked |

## 16. Risks

1. **Scope of automation.** STP and automatic placement (BRRN.031/040) could renew a policy the client did not want. Mitigation: automation starts only after explicit initiation (BRRN.021) and client acceptance. A FAIL check blocks every automatic step, and `RNW_AUTO_PLACEMENT` can be switched off.
2. **Rules content missing** (RQ01/24). Mitigation: with no active matrix, every candidate is MANUAL. The flow works fully manually as in the main BRD.
3. **Contract changes to built NB modules** (account, booking, quotation, nonpackage). Mitigation: additive columns with defaults, old constructors kept, all edits in one R0 agent, and the NB test suite as the gate.
4. **Volume and "no pagination"** (25,800 per month, one scrollable view). Mitigation: a virtualised grid with keyset chunks and server-side filters; exports as reports.
5. **Online vs offline disposition** (BRRN.018). Mitigation: both paths write the same `rnw_disposition` with the source recorded. The 3.004.4 "not in file" tag needs a declared scope.
6. **Claims module timing** (`brokerclaims`). Mitigation: the check reports INFO "claims not connected" while the bean is absent, and the matrix can treat "unknown claims" as MANUAL.
7. **Legacy policies at go-live** (EBIX / QPS). Mitigation: a legacy upload; renewals of legacy policies take the NB path until they are booked once in BIBS.
8. **Two renewal paths for submitted policies** (default hand-off vs Renewal). Mitigation: one implementation of `RenewalHandOff` at a time (`@ConditionalOnMissingBean`), one letter engine, candidate idempotent on the SBM number.
