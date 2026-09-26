# iNXT BrokerVerse - BDOI Claims (BRD-7, CLM) Build Design

Status: **proposal for review**. This design extends `docs/architecture/BROKING_ARCHITECTURE.md` and `docs/architecture/OPERATIONS_DESIGN.md`, which stay binding, and the Developer Guide. It does not change them, except through the contract changes listed in section 12.

Requirements baseline: [`BDOI_CLM_BRD_SPEC.md`](../requirements/BDOI_CLM_BRD_SPEC.md): 43 requirement IDs (BRCLM.001-043) and questions CLQ01-CLQ28. Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRCLM.043`.

## 1. Design principles

1. **The broker records and coordinates; the insurer decides and pays.** A BDOI claim is a case file: the cover it is made under, the loss, the insurer's claim numbers, reserve and settlement **as reported by the insurer**, the status, the follow-up and the documents. BDOI keeps no claims reserve and posts no journal (Q44 answered, spec section 10).
2. **The cover is the account.** A claim is recorded against an account (ARN) and a policy year, with the policy number from issuance and the cover version from the booking endorsements. Claims never re-keys policy data and never edits it (BRCLM.007, BRCLM.039 AC3).
3. **Premium status comes from the invoice ledger.** Payment and remittance status are read from `opsledger`. Claims keeps no copy of the money (BRCLM.001).
4. **Operations is reached through the parked port, not by a new call.** Claims implements `opsledger.service.port.ClaimsFeed`, which `remittance` already consumes for the claims special remittance (OQ46). No sibling module calls Claims, and Claims calls no Operations module.
5. **BDOI statuses are data; the lifecycle is code.** The 18 claim statuses and 10 settlement types are LOVs that the Unit Head maintains; each value maps to a small fixed phase (NEW, IN_PROGRESS, TEMP_CLOSED, CLOSED) that drives the code, as the broking architecture requires (section 3.2: statuses that drive code paths stay enums).
6. **Every change is history.** Status, reserve, claimant, adjuster, follow-up, insurer updates and location references each keep an immutable history plus an `AuditTrailService` entry (BRCLM.041/042 "auditable").
7. **Parked means seam, not fake** (Operations principle 5). Insurer channels, legacy migration, SSO and any claim money through BDOI get a seam and a question, not a simulation.

## 2. Decision: extend the `claims` module or build a broking claims module

**Decision: build a new module `brokerclaims`. Do not extend the insurer-side `claims` module.**

### 2.1 Why

| Aspect | Insurer-side `claims` (built, `docs/modules/CLAIMS.md`) | What BDOI needs (BRD-7) | Consequence of extending |
|---|---|---|---|
| Whose claim | The company's own liability at its share (`ClaimPolicy.sharePct`, coinsurance leader) | The client's claim against one or more insurers; BDOI has no share | Every amount and report would need a "broker mode" |
| Money | Reserve changes, settlements and recoveries are maker-checker documents posted through the accounting engine (`CLAIM_RESERVE`, `CLAIM_SETTLEMENT`, `CLAIM_RECOVERY`, `CLAIM_COINSURANCE`) and paid through payables (`CLAIM_PAYMENT`, open items) | Insurer reserve and settlement are information only (BRCLM.023/029/030); no journal, no payable | The posting paths would have to be switched off by flag in `ReserveService`, `SettlementService`, `RecoveryService`, `ClaimPostingService`: a correctness risk in a built, tested module |
| Policy source | `underwriting.PolicyQueryService` (insurer policies, risks, UW year) | `account` (ARN, policy year, risk items / locations), `booking` endorsements, `issuance` policy number, `opsledger` invoices | Would add broking dependencies to an insurer module and two policy sources to one entity |
| Status model | Enum lifecycle REGISTERED → OPEN → PARTIALLY_SETTLED → CLOSED (`ClaimStatus`) driven by approved documents | 18 BDOI statuses, role / unit restricted, temporary vs permanent closure, 10 settlement types (BRCLM.010-015/035) | A second, parallel status model on the same entity |
| Structure | One policy, one claimant party, parties from the party master, LPOs for garages | Several locations of the cover (037), several insurers and insurer claim numbers (043), insurer updates (041), insurer location references (042), diary (022) | Most BRD-7 tables are new anyway |
| Reports | Reports Book codes at the company share in base currency (PGIBR002, 018, 012 …) | BDOI claims list reports with age this stage, follow-up, handler, Marketing team (p.42-44) | No report is reusable as is |
| Tenancy | Kept for the insurer demo company and the platform; hidden from BDOI roles (Q44) | Visible to BDOI Claims, Risk and Marketing | Mixing them exposes insurer functions to BDOI users |

### 2.2 What is reused

- **Nothing from `claims` at code level.** It stays as built, and no BDOI role receives `CLAIM_VIEW`.
- **Its patterns**: the loss details shape (loss date, notification date, nature, description), the alert dedup keys, the report support style, the demo scenario runner.
- **The platform**: LOV (with the Collections attribute-table pattern), workflow (queues, assignment, case history), audit, attachments and document naming, messaging (outbox, notifications), docgen, bulk, report framework (view / export split, archive, saved variants), alerts, managed jobs, system parameters, retention framework.

## 3. Modules and links

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `brokerclaims` (new, package `com.iortatechnxt.brokerverse.brokerclaims`, tables `bcl_*`) | Claim case file on a cover; premium check and claims authorization code; locations; insurer claims, reserve and settlement as reported; insurer updates; insurer location references; statuses, settlement types and closure with the role / unit matrix; claimant, adjuster, follow-up, action plan and diary; ageing; reports and data extract; claims home; `ClaimsFeed` adapter; loss experience query for Marketing and Renewal | BRCLM.001-043 | account, booking (read), issuance (read, through account), opsledger (query, events, port), catalog (insurers, sales units), crm (client records port), nbadmin (retention port), workflow, lov, messaging, docgen, attachment, bulk, report, alert, system, audit, security, organization | V1020-V1024 (V1920-V1921 + Java demo runner) |

No other module is created. Section 12 lists the small changes to existing modules.

### 3.1 Links to the rest of BIBS

| Link | How | BRD |
|---|---|---|
| **Accounts** (cover) | `AccountQueryService.requireByArn(arn)` inside a Claims read transaction: client, product and line, insurer, period, currency, sales stamp (`SalesStamp`), risk items of kind LOCATION (`RiskItem`: address, city, province, `location_key`). The claim keeps a snapshot (`CoverSnapshot`) so reports need no join to `acc_*` | 002/003/016/037 |
| **Policies** | Policy number per policy year from the account (recorded by `issuance` `EpolicyService.confirm` → `AccountLifecycleService.recordPolicy`); cover version = number of `booking` endorsements (`BookingQueryService.endorsements(arn)`) of that policy year effective on or before the loss date; latest version computed on read | 007/039 |
| **Operations: premium** | `InvoiceLedgerQueryService.forArn(arn)`: invoices of the policy year (original, endorsements, cancellations; cancelled excluded) with `payment_status`, `remittance_status`, `dp_flag`, balance. Listens to `OpsLedgerEvents.InvoiceMovementPosted` and `RemittanceStatusChanged` to refresh the premium check and notify the handler | 001, status 11 |
| **Operations: special remittance** | `InAppClaimsFeed implements opsledger.service.port.ClaimsFeed`. Feed `CLAIMS_SPECIAL_REMIT`: one `FeedItem` per not-fully-remitted invoice of each open claim whose status carries the attribute `awaiting_premium_remittance` and changed on or after `since`; key = invoice no.; fields `claimNo`, `arn`, `policyYear`, `status`, `handler`, `statusSince`. `remittance.SpecialRemittanceService.claimsNote` then confirms the claims condition. The claim page deep-links to the special remittance request with the invoice | OQ46, OQ25, MKTID.009 |
| **Operations: claim payments through cashiering and disbursement** | **None.** The BRD has no claim money through BDOI: the insurer pays the assured by cheque or issues an LOA to the repair shop; BDOI only transmits the cheque (status 12). If CLQ10 shows that BDOI receives proceeds, the flow is a Cashiering receipt (trust money, AR) and a Disbursement payout to the claimant through the existing `payrequest` / `DisbursementGateway` path, with a new receipt type and payee class; nothing is built now (section 13) | CLQ10 |
| **Clients** | `BrokerClaimClientRecords implements crm.service.ClientRecordsProvider`: claims appear in the client 360 view (pattern of `AccountClientRecords`, `BookingClientRecords`) | 040 |
| **Documents** | Attachments with entity type `BrokerClaim` (`<Attachments entityType="BrokerClaim" …/>`), names `<CLAIM NO>_<DOCTYPE>_<n>` (`DocumentNamingService`), document types LOV `BCL_DOCUMENT_TYPE`; insurer updates and diary entries link attachments; loss advice composed from docgen template `BCL_LOSS_ADVICE` and e-mailed through `MessageService` with the send log. **Claims reports** (the documents the CSF contact centre must retrieve, BRCSF-009) are stored as attachments of the platform document type **`CLAIM_REPORT`** (cross-BRD decision D3), linked to the claim (`BrokerClaim`) and also to the account (ARN) and the client, so CSF lists them with the client's documents. Which claim documents count as claims reports is cross-BRD question XQ05 | 041, p.24-25, NFR 15.14, BRCSF-009 |
| **Retention** | `BrokerClaimRetentionProvider implements nbadmin.service.RetentionCandidateProvider`; rule row for record type `BrokerClaim`, 10 years online (p.41) | NFR |
| **Renewal** (`renewal`, designed) | `ClaimExperienceQueryService.summary(arn, policyYear)` (public read API of `brokerclaims`, built in CL1-B): claim count, open count, statuses, paid, O/S, total. **Decided (decision D4):** Renewal reads the loss experience only through this service (no port of its own); `renewal` depends on `brokerclaims`, never the reverse; the claims part of Renewal's Account History tab and CLAIMS check wait for the Claims build (CL1-B). The total-loss indicator stays open (CLQ28) | 030/040; RN p.50, 86 |

### 3.2 Dependency graph (arrows = "depends on")

```
   account   booking   catalog   crm (ClientRecordsProvider)   nbadmin (RetentionCandidateProvider)
       \        |         |       /                               /
        +--------------- brokerclaims (new) ----------------------+
                          |      \
                          |       +--> workflow, lov, messaging, docgen, attachment, bulk, report,
                          v            alert, system, audit, security, organization
                     opsledger (InvoiceLedgerQueryService, OpsLedgerEvents,
                                port ClaimsFeed <-- implemented by brokerclaims)
                          ^
                          | (uses the port, unchanged)
                     remittance
```

No cycle: nothing depends on `brokerclaims` except a future `renewal` module through `ClaimExperienceQueryService`.

## 4. Flyway plan

Allocation: **schema V1020-V1029, demo V1920-V1929** (Developer Guide range table: V1000-V1899 for later BRDs, 10 versions each; demo V1900-V1999 in the same order). Five schema versions are used, so one block is enough.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1020__brokerclaims_foundation.sql` | CL0 | Roles `CLM_OFFICER`, `CLM_TL`, `CLM_TH`, `CLM_UH`, `CLM_RISK` and the grants of section 7 (plus WORK_VIEW, ATTACHMENT_VIEW, REPORT_VIEW, CLIENT_VIEW, OPS_VIEW for the claims roles, as Collections did); `sec_permission_action` rows (area `CLAIMS`); LOV types and seeds of section 9.3; table `bcl_lov_attribute` and its seeds (status and settlement attributes, spec section 6.1-6.2); table `bcl_status_access` with the default matrix; `sys_parameter` rows; `alt_exception_code` rows; `msg_notification_event` rows; workflow `BCL_CLAIM` (`wf_stage`, `wf_transition`); docgen template `BCL_LOSS_ADVICE`; retention rule `BrokerClaim`; `DOCUMENT_TYPE` value `CLAIM_REPORT` (insert ... on conflict do nothing; its access-class rows are seeded by the attachment access-class work item, EB V1031, because V1020 runs before V1031 on a fresh database); optional `alter table lov_type add column owner_permission` (section 12.1) |
| `V1021__brokerclaims_claim.sql` | CL0 | `bcl_handler`, `bcl_claim` (every column of section 5.1, so that CL1-A and CL1-B work on a fixed schema), `bcl_claim_location`, `bcl_insurer_claim`, `bcl_status_history` |
| `V1022__brokerclaims_insurer.sql` | CL1-A | `bcl_insurer_update`, `bcl_reserve_change`, `bcl_location_ref`, search indexes on insurer claim numbers and location keys |
| `V1023__brokerclaims_activity.sql` | CL1-B | `bcl_diary_entry`, `bcl_claim_event` (field-change timeline) |
| `V1024__brokerclaims_reports.sql` | CL1-B | Report support: indexes for ageing and location queries, shared report variants (Outstanding by insurer, Past due 90) |
| V1025-V1029 | - | Free. V1025 is held for the legacy claims migration handler if CLQ14 is confirmed |
| `db/demo/V1920__demo_brokerclaims_users.sql` | CL0 | Demo users of section 7.3, their `bcl_handler` units, `BCL_REPORT_VIEW` for the demo Marketing users |
| `db/demo/V1921__demo_brokerclaims_refs.sql` | CL1-A | Insurer location references for demo property accounts |
| Java runner `brokerclaims.demo.BrokerClaimsDemoData` | CL1-A, extended by CL1-B | About 20 claims over the booked demo accounts, run through the services (section 14) |

Rules that make this safe:
- V1020 runs after V750-V762 (LOV, workflow, security, `ops_*`) and V820 / V870 on a fresh database; all are lower versions.
- **No foreign keys to `acc_*`, `bkg_*` or `ops_*` tables.** ARN, account id, item no., invoice no., insurer code and usernames are plain values, as in Operations and Collections. Foreign keys go only to `lov_*`, `wf_*`, `sec_*`, `org_branch` and the `bcl_*` tables.
- Demo V1920+ runs after every V9xx demo and after V1900-V1909 (Collections), so booked demo accounts and ledger invoices exist.

## 5. Entities (key fields)

Money is `numeric(19,2)`, rates `numeric(19,8)`, currencies `varchar(3)`. Every table has `company_id` (except child rows), the audit columns and `version`.

### 5.1 `bcl_claim` (entity `Claim`, number `BCL-<yyyy>-nnnnnn`, CLQ13)

| Group | Columns | BRD |
|---|---|---|
| Identity | `claim_no` (unique per company), `branch_id` (handling branch), `unit_code` (LOV `BCL_UNIT`), `handler` (username), `source` (BDOI_NOTICE / INSURER_REPORTED / MIGRATED), `legacy_ref` | 041, CLQ13/14 |
| Cover snapshot (`CoverSnapshot`, CL1-A) | `arn`, `account_id`, `policy_year`, `policy_no`, `cover_version_no`, `cover_version_ref`, `cover_version_at`, `product_code`, `line_code`, `client_code`, `assured_name`, `lead_insurer_code`, `period_from`, `period_to`, `sum_insured`, `sales_region`, `sales_department`, `sales_team`, `account_officer`, `cost_center`, `invoicing_branch_id`, `currency` (default cover currency, else `BCL_DEFAULT_CURRENCY`) | 003/007/009/016/039 |
| Premium check (CL1-A) | `premium_status` (PAID / UNPAID / PARTIALLY_PAID / DIRECT_PAYMENT / NO_INVOICE), `premium_checked_at`, `authorization_code` (`CAC-<yyyy>-nnnnnn`), `authorized_by`, `authorized_at`, `dp_evidence_attachment_id` | 001 |
| Loss (`LossDetails`, CL1-A) | `loss_date`, `reported_date`, `loss_nature` (LOV `BCL_LOSS_NATURE`), `claim_type` (LOV `BCL_CLAIM_TYPE`), `loss_description` (2000), `loss_place` (motor accident place), `catastrophe_code` (LOV `BCL_CATASTROPHE`), `catastrophe_event`, `claim_amount`, `deductible`, `initial_reserve` | 004/036, p.24-25, p.42 |
| Claimant (CL1-A) | `claimant_name`, `claimant_overridden`, `claimant_reason` | 006 |
| Progress (`ClaimProgress`, CL1-B) | `status_code` (LOV `BCL_CLAIM_STATUS`), `status_since`, `phase` (enum NEW / IN_PROGRESS / TEMP_CLOSED / CLOSED), `closure_kind` (TEMPORARY / PERMANENT), `settlement_type_code` (LOV `BCL_SETTLEMENT_TYPE`), `settlement_amount`, `date_settled`, `closed_on`, `adjuster_code` (LOV `BCL_ADJUSTER`), `next_follow_up_date`, `follow_up_overridden`, `next_action_plan` (2000) | 005/010-015/017-021/029/035 |

Invariants (in `Claim`): loss date within the cover period (warning outside, CLQ02); reported date between loss date and today; `claim_no` immutable; a CLOSED claim accepts only diary entries, insurer updates and reopen; one claim reference whatever the number of locations and insurers (037 AC4, 043 AC3).

### 5.2 Children

| Table | Columns | BRD |
|---|---|---|
| `bcl_claim_location` | `claim_id`, `account_item_no`, `address`, `city`, `province`, `location_key`, `description`; unique (claim, item no.). Only items of kind LOCATION of the claim's cover may be linked | 037 |
| `bcl_insurer_claim` | `claim_id`, `insurer_code`, `share_pct` (from the invoice shares, editable), `insurer_claim_no`, `reported_to_insurer_on`, `reserve_amount`, `settled_amount`, `adjuster_code`; unique (claim, insurer, insurer claim no.); index on (company, insurer, insurer claim no.) for search and the cross-claim duplicate warning | 043, 023, 029 |
| `bcl_insurer_update` | `claim_id`, `insurer_claim_id` (nullable), `update_date`, `source` (LOV `BCL_UPDATE_SOURCE`), `reference`, `remarks` (2000), `attachment_ids`, `recorded_by`, `recorded_at`; insert-only | 041 |
| `bcl_reserve_change` | `insurer_claim_id`, `previous_amount`, `new_amount`, `reason`, `changed_by`, `changed_at`; insert-only | 023/024 |
| `bcl_location_ref` | `account_id`, `arn`, `account_item_no`, `location_key`, `insurer_code`, `insurer_location_ref`, `effective_from`, `effective_to`; a change closes the current row and opens a new one; audited | 042 |
| `bcl_status_history` | `claim_id`, `from_status`, `to_status`, `from_phase`, `to_phase`, `changed_at`, `changed_by`, `remark`, `days_in_previous` | 011/027/028 |
| `bcl_diary_entry` | `claim_id`, `entry_type` (LOV `BCL_DIARY_TYPE`), `entry_at`, `due_date`, `assignee`, `text` (2000), `done_at`, `done_by` | 022/034 |
| `bcl_claim_event` | `claim_id`, `field` (REPORTED_DATE / CLAIMANT / ADJUSTER / FOLLOW_UP / ACTION_PLAN / SETTLEMENT / COVER_VERSION / AUTHORIZATION), `old_value`, `new_value`, `reason`, `changed_by`, `changed_at` | 004/006/018/019/021/039 |

### 5.3 Set-up

| Table | Columns | BRD |
|---|---|---|
| `bcl_lov_attribute` | `type_code`, `code`, `attribute`, `value`; FK to `lov_value`; attributes for `BCL_CLAIM_STATUS`: `phase`, `waiting_on`, `follow_up_days`, `awaiting_premium_remittance`; for `BCL_SETTLEMENT_TYPE`: `outcome`, `closes_claim`, `requires_settlement_amount` | 010/014 |
| `bcl_status_access` | `status_code`, `role_code`, `unit_code` (null = any); maker-checker (`AuthorizableEntity`) | 012/013 |
| `bcl_handler` | `username`, `unit_code`, `team`, `active`; the unit used by the matrix and the default assignment | 012, NFR p.37 |

## 6. Accounting events and GL entries

**None.** `brokerclaims` publishes no `BusinessEvent`, records no open item and adds no `acc_event_type`:
- the insurer reserve and settlement are the insurer's figures, kept for monitoring and loss experience (BRCLM.023/030, Q44);
- the settlement is paid by the insurer to the assured or the repair shop (p.24-26);
- the premium side is already accounted for by booking, cashiering and remittance; a claims special remittance is an ordinary remittance batch (`OPS_REMITTANCE` events, unchanged).

If CLQ10 confirms that BDOI handles claim proceeds, the entries belong to the modules that own the money, with rules configured by Comptrollership (OQ07 / AQ02 style): receipt of proceeds as trust money (Dr Cash in bank / Cr AP claim proceeds payable to claimant, cashiering) and payout (Dr AP claim proceeds / Cr Cash in bank, disbursement). Claims would only show the status. Nothing is built now.

## 7. Security

### 7.1 Permissions (added to `security.domain.Permission` in CL0; granted in V1020)

| Permission | Used for | BRD |
|---|---|---|
| `BCL_VIEW` | Claims home, worklist, claim record (read) | all |
| `BCL_COVER_VIEW` | Cover Lookup (read-only account, endorsements, invoices, claims of the cover) | 002/003 |
| `BCL_RECORD` | Record a claim; edit loss details, locations, insurer claim numbers; record insurer updates and diary entries | 003/037/041/043 |
| `BCL_AUTHORIZE` | Generate the claims authorization code (premium PAID) | 001 |
| `BCL_STATUS_UPDATE` | Change the claim status (within the role / unit matrix); correct the reported date; reopen a temporary closure | 004/011/035 |
| `BCL_CLOSE` | Permanent closure (closing settlement type) | 005/035 |
| `BCL_REOPEN` | Reopen a permanently closed claim, with reason | 035, CLQ06 |
| `BCL_CLAIMANT_OVERRIDE` | Override or input the claimant's name | 006 |
| `BCL_SETTLEMENT_UPDATE` | Set or change the requested type of settlement, settlement amount and date | 015 |
| `BCL_ADJUSTER_ASSIGN` | Set or change the adjuster / appraiser of a claim or insurer line | 018 |
| `BCL_FOLLOW_UP_OVERRIDE` | Override the next follow-up date | 019 |
| `BCL_ACTION_PLAN` | Encode the next action plan summary | 021 |
| `BCL_RESERVE_AMEND` | Amend the insurer reserve | 024 |
| `BCL_LOCATION_REF_MAINTAIN` | Maintain insurer location references (screen and upload) | 042 |
| `BCL_SETUP` | Status and settlement attributes, status access matrix, claims handler register | 010/012/014 |
| `BCL_REPORT_VIEW` | Run Claims Handling reports on screen | 026-032/034/038/040 |
| `BCL_REPORT_EXPORT` | Download / print Claims Handling reports | 040 |
| `BCL_DATA_EXTRACT` | Flat data extract for analytics | 033 |

Reassignment of claims between handlers uses the existing `WORK_ASSIGN` (workflow). LOV values (statuses, settlement types, adjusters, catastrophe codes …) are maintained through the LOV screen: see section 12.1 for the Unit Head's right to do so.

### 7.2 Roles (V1020)

| Permission | `CLM_OFFICER` (Claims Officer / Assistant) | `CLM_TL` (Team Lead) | `CLM_TH` (Team Head) | `CLM_UH` (Unit Head) | `CLM_RISK` (Claims / Risk user) | `MKT_AO` | `MKT_TL` | `SYSADMIN`, `AUDITOR` |
|---|---|---|---|---|---|---|---|---|
| BCL_VIEW | x | x | x | x | x | | | x |
| BCL_COVER_VIEW | x | x | x | x | x | | | |
| BCL_RECORD | x | x | x | | | | | |
| BCL_AUTHORIZE | x | x | x | | | | | |
| BCL_STATUS_UPDATE | x (matrix: newly filed and temporary closure statuses) | x | x | | | | | |
| BCL_CLOSE | | x | x | | | | | |
| BCL_REOPEN | | | x | x | | | | |
| BCL_CLAIMANT_OVERRIDE, BCL_SETTLEMENT_UPDATE, BCL_ADJUSTER_ASSIGN, BCL_FOLLOW_UP_OVERRIDE, BCL_RESERVE_AMEND | | x | x | | | | | |
| BCL_ACTION_PLAN | x | x | x | | | | | |
| BCL_LOCATION_REF_MAINTAIN | x | x | x | | | | | |
| BCL_SETUP | | | | x | | | | |
| BCL_REPORT_VIEW | x | x | x | x | x | x | x | x |
| BCL_REPORT_EXPORT | x | x | x | x | x | | x (CLQ15) | |
| BCL_DATA_EXTRACT | | | | x | x | | | |
| WORK_ASSIGN | | x | x | x | | | | |

The matrix follows the stakeholder table (p.28). The officer's status rights are limited by the seeded `bcl_status_access` rows until BDOI gives the matrix (CLQ04, CLQ06).

### 7.3 Demo users (V1920, password `Brokerverse@2026`)

| User | Role | Unit |
|---|---|---|
| `clmofficer` | CLM_OFFICER | MOTOR_HO |
| `clmofficer2` | CLM_OFFICER | NON_MOTOR_HO |
| `clmbranch` | CLM_OFFICER | BRANCH_CEBU |
| `clmtl` | CLM_TL | MOTOR_HO |
| `clmth` | CLM_TH | NON_MOTOR_HO |
| `clmuh` | CLM_UH | - |
| `clmrisk` | CLM_RISK | - |

The existing insurer demo user `claims` is unrelated and keeps its insurer roles.

## 8. Status model and workflow

### 8.1 Phases (enum `ClaimPhase`) and BDOI statuses (LOV)

```
                 set status (phase IN_PROGRESS)                set status (phase TEMP_CLOSED)
   NEW ─────────────────────────────────────> IN_PROGRESS ──────────────────────────────────> TEMP_CLOSED
    │                                             │   ^                                            │
    │                                             │   └──────── set status (phase IN_PROGRESS) ────┘  (BCL_STATUS_UPDATE)
    │   closing settlement type (BCL_CLOSE)       │ closing settlement type (BCL_CLOSE)
    └─────────────────────────────────────────> CLOSED <───────────────────────────────── (from TEMP_CLOSED too)
                                                  │ reopen (BCL_REOPEN, reason) → IN_PROGRESS
```

- **Status change** (`ClaimStatusService.change(claimId, statusCode, remark)`): requires `BCL_STATUS_UPDATE` (`BCL_RECORD` for the first status at recording); the status must be effective and allowed by `bcl_status_access` for one of the user's roles and the user's unit (`BCL_STATUS_NOT_ALLOWED`, BRCLM.012/013); a CLOSED claim refuses it. Effects: `bcl_status_history` row; `status_since` reset (age this stage); next follow-up date recomputed (status `follow_up_days`, else `BCL_FOLLOW_UP_DAYS`) unless overridden; phase change → workflow system transition; status with `awaiting_premium_remittance` → the claim appears in the `CLAIMS_SPECIAL_REMIT` feed and shows the unremitted invoices with the special remittance link.
- **Settlement** (`ClaimSettlementService.set(claimId, type, amount, date)`): `BCL_SETTLEMENT_UPDATE`; a type with `requires_settlement_amount` needs amount and date; a type with `closes_claim` also needs `BCL_CLOSE` and makes the claim CLOSED / PERMANENT with `closed_on`.
- **Temporary closure**: statuses 17 and 18 (phase TEMP_CLOSED) set `closure_kind` TEMPORARY (BRCLM.035). Ageing continues (the claim is outstanding) unless BDOI decides otherwise (CLQ06).
- **Reopen** of a CLOSED claim: `BCL_REOPEN` with reason (LOV `BCL_REOPEN_REASON`); settlement type kept in history, cleared on the claim.

### 8.2 Workflow `BCL_CLAIM` (seeded in V1020)

Stages NEW (initial, owner `BCL_RECORD`, SLA 24 h), IN_PROGRESS (owner `BCL_RECORD`), TEMP_CLOSED (no owner), CLOSED (no owner, not terminal because of reopen). Transitions `progress`, `temp_close`, `resume`, `close`, `reopen` are business actions called only by the Claims services (`systemTransition` after their own permission checks). The case gives the My Work queue per handler, assignment and reassignment (`WorkAssignmentService`, `WORK_ASSIGN`) and the case history. The detailed status history stays in `bcl_status_history`, which the ageing reports need.

### 8.3 Premium check and authorization code (BRCLM.001)

`PremiumCheckService.check(claim)`:
1. invoices of the ARN and policy year from `InvoiceLedgerQueryService.forArn(arn)`, cancelled invoices excluded;
2. none → NO_INVOICE; any UNPAID → UNPAID; any PARTIALLY_PAID → PARTIALLY_PAID; all DP (`dp_flag`, payment status NOT_APPLICABLE) → DIRECT_PAYMENT; else PAID;
3. result, time and the unpaid invoice list (invoice no., kind, balance) returned to the screen; alert `BCL_UNPAID_PREMIUM_CLAIM` when a claim is recorded or stays on an unpaid cover.

`authorize(claimId)` (`BCL_AUTHORIZE`) issues `CAC-<yyyy>-nnnnnn` only when the check is PAID, or DIRECT_PAYMENT under parameter `BCL_AUTH_DP_POLICY` (ALLOW / CONFIRM = insurer payment evidence attached / BLOCK; default CONFIRM). Otherwise `BCL_PREMIUM_UNPAID` (HTTP 422) and the button is disabled. The check re-runs on `InvoiceMovementPosted` for the ARN and in the daily job. The meaning and use of the code wait for CLQ01.

## 9. Jobs, alerts, parameters, LOVs, numbers, templates, bulk handlers

### 9.1 Jobs (`ManagedJob`; crons in `application.yml` as `brokerverse.jobs.bcl-*-cron`, documented in `docs/operations/CONFIGURATION.md`)

| Job | Default schedule | Does |
|---|---|---|
| `BCL_FOLLOW_UP_DUE` | daily 06:00 Manila | Notifies handlers of claims whose next follow-up date or diary due date is today; raises `BCL_FOLLOW_UP_OVERDUE` for past dates (BRCLM.019/022/034) |
| `BCL_PREMIUM_RECHECK` | daily 05:30 Manila | Re-runs the premium check of open, unauthorised claims (safety net for missed events) (BRCLM.001) |
| `BCL_AGEING_ALERTS` | daily 06:00 Manila | Raises `BCL_CLAIM_PAST_DUE` for outstanding claims older than `BCL_PAST_DUE_DAYS` (p.43) |

### 9.2 Alerts (`alt_exception_code`, seeded in V1020)

| Code | Severity | Threshold | Raised when | Dedup key |
|---|---|---|---|---|
| `BCL_UNPAID_PREMIUM_CLAIM` | MEDIUM | - | A claim is on a cover with unpaid or partly paid premium | `BCL_UNPAID_PREMIUM_CLAIM:<claim id>` |
| `BCL_FOLLOW_UP_OVERDUE` | LOW | 0 days | Next follow-up date or a diary due date has passed | `BCL_FOLLOW_UP_OVERDUE:<claim id>` |
| `BCL_CLAIM_PAST_DUE` | MEDIUM | 90 days | Outstanding claim older than the threshold | `BCL_CLAIM_PAST_DUE:<claim id>` |
| `BCL_INSURER_CLAIM_NO_REUSED` | LOW | - | An insurer claim number already exists on another claim for the same insurer | `BCL_INSURER_CLAIM_NO_REUSED:<insurer>:<number>` |

### 9.3 Parameters and LOVs

Parameters (`sys_parameter`): `BCL_DEFAULT_CURRENCY` (STRING, PHP); `BCL_FOLLOW_UP_DAYS` (INTEGER, 7); `BCL_AGEING_BUCKETS` (INTEGER_LIST, 30,60,90,180 → 0-30 / 31-60 / 61-90 / 91-180 / 181+); `BCL_PAST_DUE_DAYS` (INTEGER, 90); `BCL_PRONE_MIN_CLAIMS` (INTEGER, 3); `BCL_PRONE_YEARS` (INTEGER, 3); `BCL_AUTH_DP_POLICY` (STRING, CONFIRM).

LOV types (seeds marked "to confirm" where the BRD gives none):
- `BCL_CLAIM_STATUS` (18 values, spec 6.1) and `BCL_SETTLEMENT_TYPE` (10 values, spec 6.2), with `bcl_lov_attribute`;
- `BCL_ADJUSTER` (25 values, spec 6.3);
- `BCL_CATASTROPHE` (7 values, spec 6.4);
- `BCL_LOSS_NATURE`, `BCL_CLAIM_TYPE` (to confirm, CLQ12; seeds: Motor own damage, Motor third party, Motor theft, Fire, Property, Engineering, Marine, Liability, Personal accident, Others);
- `BCL_UNIT` (MOTOR_HO, NON_MOTOR_HO, BRANCH_ANGELES, BRANCH_CEBU, BRANCH_CDO, BRANCH_DAVAO, BRANCH_GENSAN; CLQ04);
- `BCL_UPDATE_SOURCE` (EMAIL, LETTER, PORTAL, CALL, FILE; CLQ17);
- `BCL_DIARY_TYPE` (CALL, EMAIL, MEETING, NOTE, FOLLOW_UP);
- `BCL_DOCUMENT_TYPE` (PLA, CRF, estimate, offer, signed offer, LOA, release papers, others; CLQ26);
- `BCL_REOPEN_REASON`, `BCL_OVERRIDE_REASON`.

### 9.4 Numbers, templates, notifications

- Numbers (`DocumentNumberService`): claim `BCL-<yyyy>`; authorization code `CAC-<yyyy>`.
- Docgen: `BCL_LOSS_ADVICE` (formal loss advice / PLA with the p.24-25 fields and the insurer claim numbers), sent from the claim with `SendEmailDialog` to the insurer's claims e-mail; send log per claim.
- Notification events (`msg_notification_event`): `BCL_FOLLOW_UP_DUE` (handler), `BCL_CLAIM_ASSIGNED` (new handler), `BCL_STATUS_CHANGED` (account officer, in-app; e-mail to client / AO / insurer only for the events BDOI names, CLQ22), `BCL_PREMIUM_REMITTED` (handler, on `RemittanceStatusChanged` to FULLY_REMITTED), `BCL_NEWER_COVER_VERSION` (handler, on `OpsInvoiceBooked` of an endorsement of the cover).

### 9.5 Bulk handlers (`BulkImportHandler`)

| Handler | Permission | Columns | BRD |
|---|---|---|---|
| `BCL_INSURER_UPDATE` | BCL_RECORD | claim no. or (insurer, insurer claim no.), update date, source, reference, remarks | 041 |
| `BCL_INSURER_CLAIM_NO` | BCL_RECORD | claim no., insurer, insurer claim no., reported to insurer on | 043 |
| `BCL_LOCATION_REF` | BCL_LOCATION_REF_MAINTAIN | ARN, item no., insurer, insurer location reference, effective from | 042 |
| `BCL_CLAIM_MIGRATION` | BCL_SETUP | legacy claim fields | parked (CLQ14), V1025 |

## 10. Reports (category `CLAIMS_HANDLING`, view `BCL_REPORT_VIEW`, export `BCL_REPORT_EXPORT`)

One `ReportDefinition` per report in `brokerclaims.report`, SQL aggregates over `bcl_*` (and `ops_invoice` for premium). Common parameters: company, branch, unit, handler, insurer, product line, Marketing team, AO, loss / reported date range, as-of date. Formats XLSX (the BRD format), PDF, CSV. File name `<Report>_<date of extraction>` (Report List).

| Code | Title | Rules | BRD |
|---|---|---|---|
| `BCL-OUTSTANDING` | List of all Outstanding Claims | Phases NEW, IN_PROGRESS, TEMP_CLOSED as of the date; the p.42 columns (claim number, claimant, assured, nature / type of loss, loss and reported dates, claim amount, deductible, status, insurer(s), claim type, follow-ups / remarks = action plan, age this stage, age overall, next follow-up, handler, Marketing team, AO); insurer claim numbers | 031 |
| `BCL-OUTSTANDING-PAST-DUE` | Outstanding Claims 90 Days Past Due | As above, age overall > `days` (default `BCL_PAST_DUE_DAYS`) | 031, p.43 |
| `BCL-SETTLED` | List of all Settled Claims | Settled outcome, date settled in the range; p.42 columns with date settled and settlement amount | 029 |
| `BCL-AGEING` | Claims Aging (overall) | Outstanding claims with age overall and bucket; totals per bucket and insurer | 025/026 |
| `BCL-AGEING-STATUS` | Claims Aging per Status | Grouped by status, then insurer; age this stage and overall; per-status time spent from `bcl_status_history` | 027/028 |
| `BCL-LOSS-EXPERIENCE` | Loss Experience | Per claim and insurer line: insured, claimant, policy no., loss date, nature, type, deductible, paid = settled amount, O/S = max(reserve - paid, 0) while open, total, insurer, status; grouped by client > ARN > policy year | 030 |
| `BCL-LOSS-RATIO` | Loss Ratio | Loss experience totals / premium × 100 per cover and policy year; premium = signed gross premium of the ARN's ledger invoices of that year (originals, endorsements, cancellations); grouping client, line or insurer (CLQ20) | 032 |
| `BCL-PENDING-ACTIONS` | Pending Actions | Claims with follow-up due or overdue, open diary entries due, per handler | 034 |
| `BCL-PRONE-LOCATIONS` | Claims by Location / Claims-prone Locations | Count, paid, O/S per location key, city and province over the period; flag at or above `BCL_PRONE_MIN_CLAIMS` in `BCL_PRONE_YEARS`; catastrophe filter; drill-down | 038 |
| `BCL-INSURER-CLAIMS` | Insurer Claim Numbers | One row per insurer claim number with the BDOI claim, insurer, share, reserve, settled | 043 |
| `BCL-ACTIVITY-LOG` | Claims Activity Log | Status history, field changes, insurer updates, location reference changes, diary; user and time | 041/042, NFR 15.08 |
| `BCL-DATA-EXTRACT` | Claims Data Extract | Flat, one row per claim × insurer line × location with every field; permission `BCL_DATA_EXTRACT` for view and export | 033 |

Saved report variants (`nbreport` `ReportVariantService`) work for every code. Marketing reaches the loss reports with view only unless granted export (BRCLM.040).

## 11. Screens (group **Claims & Insurance**, section **Claims Handling**, listed first)

Frontend folder `frontend/src/features/brokerclaims`, module id `brokerclaims`, route root `/claims-handling`, help section `BROKER_CLAIMS_HELP`. Screens follow `docs/design/BDO_UX_GUIDELINES.md` (work list card with status tabs and `WorklistToolbar`, `RecordSummary`, boxed tabs, flag chips separate from the status pill).

| Route | Screen | Permission | BRD |
|---|---|---|---|
| `/claims-handling` | Claims home: my open claims, follow-ups due today / overdue, claims by status and phase, ageing buckets, unpaid-premium claims, awaiting premium remittance (NFR 15.03 dashboard) | BCL_VIEW | 025/034 |
| `/claims-handling/worklist` | Worklist with tabs My Claims, Open, Temporarily Closed, Closed, Follow-ups Due; search by claim no., insurer claim no., ARN, policy no., assured; bulk reassign (WORK_ASSIGN) | BCL_VIEW | 034, 043 AC5 |
| `/claims-handling/new` | Record Claim: cover search (ARN / policy no. / assured) → cover card (policy no., version, period, premium check with unpaid invoices, Marketing team, AO, branch) → locations picker → loss details → insurers (from the invoice shares) | BCL_RECORD | 001/003/004/006/009/016/037/039 |
| `/claims-handling/:id` | Claim record: summary card (claim no. chip, status pill, flags *Unpaid premium*, *Awaiting premium remittance*, *Newer cover version*, *Multi-location*, *Multi-insurer*, *CAT*); actions Change Status, Set Settlement, Close, Reopen, Generate Authorization Code, Send Loss Advice, Request Special Remittance (link); tabs Details, Locations, Insurers & Updates, Reserve & Settlement, Diary, Documents, History | BCL_VIEW (+ action permissions) | all |
| `/claims-handling/covers` | Cover Lookup (read-only) with the claims of the cover and the insurer location references | BCL_COVER_VIEW | 002/003/042 |
| `/claims-handling/location-refs` | Insurer location references: search, maintain, upload | BCL_LOCATION_REF_MAINTAIN | 042 |
| `/claims-handling/diary` | My Diary across claims | BCL_VIEW | 022 |
| `/claims-handling/setup` | Status and settlement attributes, status access matrix, handler register (LOV values themselves on Broking Setup → LOVs) | BCL_SETUP | 010/012/014/017/036 |
| `/claims-handling/reports` | Claims Handling reports landing (Report Centre filtered by category) | BCL_REPORT_VIEW | 026-034/038/040 |

Outside the module (frontend only, owned by their modules, section 12.2): a **Claims** tab on the account page and a loss summary on the client 360 view.

## 12. Impact on modules already built or being built

### 12.1 Platform modules

| Module | Change | Why | Files |
|---|---|---|---|
| `security` | 18 `BCL_*` permissions | Section 7.1 | `security/domain/Permission.java` (CL0 only) |
| `report` | `ReportCategory.CLAIMS_HANDLING("Claims Handling")`; `ReportMetadata.claimsHandling(code, title, …)` factory (view `BCL_REPORT_VIEW`, export `BCL_REPORT_EXPORT`), as `ReportMetadata.collections(...)` | Section 10 | `report/core/ReportCategory.java`, `report/core/ReportMetadata.java` (CL0) |
| `lov` (optional, recommended) | `lov_type.owner_permission` (nullable): when set, maintaining the type's values requires that permission instead of `LOV_MANAGE`; claims LOV types set it to `BCL_SETUP`. Without this change the Claims Unit Head needs the global `LOV_MANAGE` | BRCLM.010/014/017/036: the Unit Head maintains the values (p.28) | `lov/domain/LovType.java`, `lov/service/LovService.java`, V1020 (CL0); applies to Collections LOVs too if they want it |
| `workflow`, `messaging`, `docgen`, `attachment`, `bulk`, `alert`, `system`, `audit` | None (seed rows only, in V1020) | - | - |
| `claims` (insurer-side) | None. Stays hidden from BDOI roles | Section 2 | - |
| GL (`accounting`, `journal`, `ledger`, `subledger`, `coa`, `period`), `payables`, `receivables`, `tax` | None | Section 6 | - |
| Navigation and help | Section `Claims Handling` first in group `insurance` | Section 11 | `frontend/src/navigation/modules.ts`, `frontend/src/features/help/helpContent.ts` (CL0) |
| Configuration | Crons `brokerverse.jobs.bcl-follow-up-due-cron`, `bcl-premium-recheck-cron`, `bcl-ageing-alerts-cron` | Section 9.1 | `application.yml`, `docs/operations/CONFIGURATION.md` (CL0) |

### 12.2 Broking, Operations and later-BRD modules

| Module | Change | Contract |
|---|---|---|
| `account` (built) | **No backend change**: Claims reads `AccountQueryService.requireByArn` and `Account.getItems()` inside its own read-only transaction. **UI ask**: a Claims tab on the account page (`features/accounts`) calling `GET /api/v1/broker-claims/experience?arn=` (hidden without `BCL_VIEW` / `BCL_REPORT_VIEW`) | Ask to the account owner |
| `booking` (built) | None: `BookingQueryService.endorsements(arn)` gives the cover versions | - |
| `issuance` (built) | None: the policy number is on the account | - |
| `opsledger` (built) | None in code: `ClaimsFeed`, `FeedItem`, `InvoiceLedgerQueryService.forArn`, `OpsLedgerEvents.InvoiceMovementPosted / RemittanceStatusChanged / OpsInvoiceBooked` exist. Doc update: `docs/modules/OPERATIONS.md` 1.3 row `ClaimsFeed` → "replaced by `brokerclaims.InAppClaimsFeed` (BRD-7)" | Docs only |
| `remittance` (built) | **No code change, one behaviour change to agree**: once `InAppClaimsFeed` is a bean, `SpecialRemittanceService.claimsNote` refuses a CLAIMS-condition request when no claim of the invoice's cover is in a status flagged `awaiting_premium_remittance` (`SPECIAL_REMIT_NOT_ELIGIBLE`). Today it only adds a note. **UI ask**: the special remittance form accepts `?invoiceNo=&condition=CLAIMS` so Claims can deep-link | Agree with the remittance owner before CL1-A merges; OPERATIONS.md 3.5 note |
| `cashiering`, `commission`, `adjustment`, `prodrecon` (built) | None. Claim proceeds through BDOI would add a receipt type to cashiering (CLQ10, parked) | - |
| `collections` (being built, BRD-4) | None. Contract note: the billing statement (BRCLXN.058) shows the policy number (BRCLM.008) | Note to C1-B |
| `disbursement`, `payrequest`, `acsl`, `frbs` (being built, BRD-5) | None. A claimant payee class would be needed only if CLQ10 is answered yes | - |
| `crm` (built) | None: `brokerclaims` implements `ClientRecordsProvider` | - |
| `nbadmin` (built) | None: `brokerclaims` implements `RetentionCandidateProvider`; rule row seeded in V1020 | - |
| `catalog` / `productmaint` (built) | None: insurer panel read through `InsurerService`; sales units read from the account stamp | - |
| `nbreport` (built) | None: saved variants reused | - |
| `renewal` (Renewal BRD, designed) | Consumes `brokerclaims.service.ClaimExperienceQueryService.summary(arn, policyYear)` for "With Claim (Y/N)", number and status of claims (RN p.50, p.86), the CLAIMS check, the decision matrix and the Account History tab; the "Total Loss Claim" non-renewal reason (RN p.82) needs a total-loss indicator that BRD-7 does not define (CLQ28, still open) | **Decided (D4)**; RENEWAL_DESIGN section 2.3 |
| `csf` (Customer Servicing Facility, designed) | Claims reports (BRCSF-009) stored as `CLAIM_REPORT` attachments linked to the claim, the account and the client (section 3.1); the claim itself stays reachable through the client records port | **Decided (D3)**; CUSTOMER_SERVICING_DESIGN section 11 |

## 13. Integrations to park (seam only)

| Item | BRD | Seam built now | Question |
|---|---|---|---|
| Insurer channels for claims (portal, API, SFTP bordereaux) | 041, 043 | E-mail loss advice with send log; manual recording and bulk upload of insurer updates and claim numbers | CLQ17 |
| Legacy EBIX / ISYS claims and policies | 007, 038, p.23 | `source = MIGRATED`, `legacy_ref`; migration handler slot V1025 | CLQ14 |
| BDO SSO / Active Directory (Windows credentials) | NFR 1.01 | Q42 answered by BRD-11: directory sign-in is required and is built as the parked `security` port `DirectoryAuthenticator` (USER_ACCESS_DESIGN section 10); local sign-in stays until BDO supplies the EUA interface | UQ04 |
| Shared drive for report files | NFR 15.09-15.13 | Report archive in-app (`report_run`); `FileDropPort` if BDOI confirms the drive | CLQ21, OQ17 |
| Claim proceeds through BDOI | status 12 | None; flow described in sections 3.1 and 6 | CLQ10 |
| Hazard / geocoding data for claims-prone areas | 038 | Location key, city, province only | CLQ16 |
| Adjuster directory with contacts | 017/018 | LOV only | CLQ11 |

## 14. Build-wave plan

Prerequisites: BRD-1 `account`, `booking`, `issuance`; Operations `opsledger` and `remittance` (all built). Claims does not wait for Collections or BRD-5.

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **CL0** (1 agent, short) | Foundation | Permissions; V1020 (roles, grants, actions, LOVs + `bcl_lov_attribute`, status matrix, parameters, alerts, notification events, workflow `BCL_CLAIM`, template, retention rule, optional `lov_type.owner_permission`); V1021 (claim tables); `Claim`, `ClaimRepository` and empty embeddables `CoverSnapshot`, `LossDetails`, `ClaimProgress` mapped to the V1021 columns; `ReportCategory` / `ReportMetadata` factory; optional `lov` owner permission; package skeleton; navigation entry and help registration (landing screen); crons; demo users V1920 | `security/domain/Permission.java`, `report/core/ReportCategory.java`, `report/core/ReportMetadata.java`, `lov/**` (owner permission only), `brokerclaims/package-info.java`, `brokerclaims/domain/Claim*.java`, V1020, V1021, V1920, `frontend/src/navigation/modules.ts`, `frontend/src/features/help/helpContent.ts`, `frontend/src/features/brokerclaims/module.ts` (landing), `application.yml`, `docs/operations/CONFIGURATION.md`, Developer Guide range row | `mvn verify` green; the Hibernate schema validates; the landing screen shows for `clmofficer` |
| **CL1-A** | Claim record and insurers | Cover lookup and snapshot, premium check and authorization, record claim, loss details, claimant, locations, insurer claims and reserve, insurer updates, location references, loss advice e-mail, `InAppClaimsFeed`, ops event listeners, `ClientRecordsProvider`, retention provider, bulk handlers, API, screens Record Claim / Cover Lookup / Location refs, record tabs Details / Locations / Insurers & Updates / Reserve & Settlement (reserve part) / Documents, demo V1921 and `BrokerClaimsDemoData` | `brokerclaims/{cover,claim,insurer,location,feed}/**`, `CoverSnapshot`, `LossDetails`, V1022, V1921, `features/brokerclaims/{record,cover,insurer,location}/**` | Claim on an unpaid cover blocked, authorised after payment; multi-location and multi-insurer claim; special remittance request confirmed through the feed (remittance test) |
| **CL1-B** | Status, follow-up and reports | Status engine with the matrix, settlement and closure, reopen, workflow, follow-up and action plan, diary, jobs and alerts, setup screens, claims home and worklist, record tabs Diary / History and the status actions, `ClaimExperienceQueryService`, all reports of section 10 | `brokerclaims/{status,diary,report,home,setup}/**`, `ClaimProgress`, V1023, V1024, `features/brokerclaims/{home,worklist,status,diary,setup,reports}/**`, `features/brokerclaims/help.ts` | Status refused outside the matrix; temporary and permanent closure; ageing per status; every report runs and exports XLSX / PDF / CSV |
| **CL2** (1 agent) | Integration | E2E: book → claim blocked by unpaid premium → cashiering application → authorization → "With BDOI - For Premium Remittance" → special remittance → remittance batch → FULLY_REMITTED notification → settled (LOA) → reports; Marketing report access; module guide `docs/modules/BROKER_CLAIMS.md`; account-page Claims tab (with the account owner); demo storyline completed; migration handler if CLQ14 is answered | tests, docs, demo, `features/accounts` Claims tab (agreed) | Full `mvn verify` / `npm run verify`; walkthrough with the demo users |

Rules for parallel work:
- One Flyway set per agent (section 4); no edits to another agent's packages.
- Shared files (Permission enum, `ReportCategory` / `ReportMetadata`, navigation, help registry, `application.yml`, V1020, V1021) are edited **only in CL0**.
- `Claim` is owned by CL0; CL1-A owns `CoverSnapshot` and `LossDetails`, CL1-B owns `ClaimProgress`. A business method on `Claim` delegates to the owner's embeddable.
- `features/brokerclaims/module.ts` is created by CL0 with every route declared and lazy components pointing at placeholder pages; CL1-A and CL1-B replace only their own pages. `help.ts` is owned by CL1-B, and CL1-A sends its entries to CL1-B.
- Each agent has its own `*ApiIT` smoke class instead of editing the shared `ApiSmokeIT`.
- CL1-A and CL1-B agree the `ClaimStatusChanged` Spring event (claim id, from / to status and phase) in CL0; CL1-A's feed reads the status only through `ClaimProgress` getters.

Effort: CL0 small; CL1-A medium-large (about 35 % of the build); CL1-B medium; CL2 small. The module is about the size of `adjustment`.

## 15. What depends on information BDOI has not given (build the seam, park the content)

| Topic | Requirements | Built now | Parked content | Q |
|---|---|---|---|---|
| Claims authorization code | 001 | Premium check, code generation, parameter for direct payment | Meaning and use of the code, overrides | CLQ01 |
| Cover version per location | 037/039 | Version = endorsement count; location snapshot | Location-level endorsement history | CLQ02 |
| Status matrix, units, follow-up days | 010-013/019 | Tables with a default matrix and 7 days | BDOI matrix and values | CLQ04, CLQ07 |
| Settlement types vs settlement status | 014/015 | 10 types with attributes | Second list, if any | CLQ05 |
| Closure and reopen rights | 005/035 | Officer: temporary; TL / TH: permanent; TH / UH: reopen | Confirmation | CLQ06 |
| Reserve and loss experience definitions | 023/030/032 | Reserve per insurer line; O/S = reserve - paid; premium = signed gross premium | Definitions and bases | CLQ08, CLQ09, CLQ20 |
| Claim type, loss nature, CNR | 010/036 | LOVs with provisional seeds | Values | CLQ12 |
| Marketing access scope | 040 | View for AO / TL, export for TL | Final grants, portfolio restriction | CLQ15 |
| Claims-prone thresholds | 038 | Parameters (3 claims in 3 years) | Values, granularity | CLQ16 |
| Notifications and templates | NFR 15.14 | Events and one loss-advice template | Events and wording | CLQ22 |

## 16. Risks

1. **The authorization code is not what BDOI means** (CLQ01). Mitigation: the premium check is useful on its own; the code is one field and one action, cheap to rename or drop.
2. **Remittance behaviour change.** Connecting `ClaimsFeed` turns a note into a hard rule for claims special remittances. Mitigation: agree it with the remittance owner and Operations users before CL1-A merges; a parameter can keep the "note only" behaviour during transition.
3. **Unknown status matrix** (CLQ04). Mitigation: the matrix is data; the default grants TL / TH every status and officers the recording and temporary-closure statuses.
4. **History for analytics.** Claims-prone and loss-ratio analysis are weak without legacy claims (CLQ14). Mitigation: the migration slot and `source = MIGRATED`; the reports work on BIBS data from day one.
5. **Cover version semantics** (CLQ02). If BDOI needs location lists as at the loss date, `account` or `adjustment` must version risk items: a change outside this module, to be designed with the BRD-1 / Operations owners.
6. **Overlap with Renewal and CSF.** Mitigation: one read API (`ClaimExperienceQueryService`) and the client records port; neither module writes claims.

## 17. CL0 foundation: as built

What the CL0 wave built, and where it details or differs from the sections above. CL1-A and CL1-B build on this and
compile only against it.

- **Permissions.** 18 constants `BCL_*` in `security.domain.Permission` (section 7.1), granted in V1020 as in 7.2. The
  claims roles also get `WORK_VIEW`, `ATTACHMENT_VIEW`, `REPORT_VIEW`, `CLIENT_VIEW`, `ACCOUNT_VIEW`, `OPS_VIEW`;
  `CLM_OFFICER`, `CLM_TL`, `CLM_TH` also `ATTACHMENT_MANAGE` (Documents tab) and `BULK_PROCESS` (insurer update, insurer
  claim number and location reference uploads). `sec_permission_action` area `CLAIMS`: VIEW (view, cover view, report
  view / export, data extract), CREATE (record, authorize, location refs), AMEND (record, location refs, status update,
  claimant, settlement, adjuster, follow-up, reserve, action plan, setup), APPROVE (close, reopen).
- **Migration `V1020__brokerclaims_foundation.sql`.**
  - Roles `CLM_OFFICER`, `CLM_TL`, `CLM_TH`, `CLM_UH`, `CLM_RISK`; Marketing `MKT_AO` + BCL_REPORT_VIEW, `MKT_TL` +
    BCL_REPORT_VIEW, BCL_REPORT_EXPORT; `SYSADMIN` and `AUDITOR` + BCL_VIEW, BCL_REPORT_VIEW.
  - `lov_type.owner_permission` (section 12.1, built): the holders of the owner permission may list, add, change,
    deactivate and authorize (maker-checker kept) the values of that list through the existing `/api/v1/lov` API, in
    addition to `LOV_MANAGE` / `MASTER_AUTHORIZE` (kept, so administrators still maintain every list). Bean
    `lov.service.LovAccess` (`lovAccess`) in the `@PreAuthorize` of `LovController`; `LovTypeResponse.ownerPermission`.
    Every `BCL_*` list has owner permission `BCL_SETUP`.
  - LOV types and codes: `BCL_CLAIM_STATUS` (18: `NEW_COMPLETE_DOCS`, `NEW_INCOMPLETE_DOCS`, `ADJUSTER_REVIEW`,
    `CLAIMANT_OFFER_ACCEPTANCE`, `CLAIMANT_DOCS_SUBMISSION`, `INSURER_CHECK_ISSUANCE`, `INSURER_RELEASE_PAPERS`,
    `INSURER_REVIEW`, `ADJUSTER_SETTLEMENT_OFFER`, `ASSURED_MEETING`, `BDOI_PREMIUM_REMITTANCE`,
    `BDOI_CHECK_TRANSMITTAL`, `BDOI_UNDER_REVIEW`, `CLAIMANT_SALVAGE_PULLOUT`, `CLAIMANT_CNR_SUBMISSION`,
    `INSURER_LOA_ISSUANCE`, `TEMP_CLOSED_NO_DOCS`, `TEMP_CLOSED_WITH_OFFER`, in the order of spec 6.1),
    `BCL_SETTLEMENT_TYPE` (10: `CLOSED_CANCELLED`, `CLOSED_DENIED`, `CLOSED_WITHIN_DEDUCTIBLE`,
    `CLOSED_WITHOUT_PAYMENT`, `SETTLED`, `SETTLED_DIRECTLY_FILED`, `SETTLED_LOA_ISSUED`, `SETTLED_LOA_REPAIR_SCHEDULE`,
    `SETTLED_LOA_UNDER_REPAIR`, `SETTLED_RELEASE_PAPERS`), `BCL_ADJUSTER` (25), `BCL_CATASTROPHE` (7), `BCL_LOSS_NATURE`
    and `BCL_CLAIM_TYPE` (10 provisional each, CLQ12), `BCL_UNIT` (7), `BCL_UPDATE_SOURCE`, `BCL_DIARY_TYPE`,
    `BCL_DOCUMENT_TYPE` (parent = `DOCUMENT_TYPE` code, incl. `CLAIM_REPORT`), `BCL_REOPEN_REASON`,
    `BCL_OVERRIDE_REASON`; platform `DOCUMENT_TYPE` value `CLAIM_REPORT` (on conflict do nothing).
  - `bcl_lov_attribute` (type, code, attribute, value; FK to `lov_value`): every status has `phase` and `waiting_on`;
    `BDOI_PREMIUM_REMITTANCE` has `awaiting_premium_remittance = true`; no status has `follow_up_days` yet (parameter
    `BCL_FOLLOW_UP_DAYS` applies). Every settlement type has `outcome`, `closes_claim`, `requires_settlement_amount`;
    `SETTLED_LOA_REPAIR_SCHEDULE` and `SETTLED_LOA_UNDER_REPAIR` have `closes_claim = false` until CLQ05.
  - `bcl_status_access` (status_code, role_code FK `sec_role.code`, unit_code null = any; maker-checker columns of
    `AuthorizableEntity`; unique status + role + unit): officers the four statuses of phases NEW and TEMP_CLOSED, TL and
    TH all 18. No company column: the matrix is BDOI-wide.
  - `bcl_handler` (username unique, unit_code FK `BCL_UNIT`, team, active). No company column.
  - Parameters (category `CLAIMS_HANDLING`), alert codes and notification events (module `CLAIMS_HANDLING`, event sort
    orders 400-440) of section 9: the insurer-side module keeps module `CLAIMS`.
  - Workflow `BCL_CLAIM`: stages `NEW` (initial, owner BCL_RECORD, SLA 24 h), `IN_PROGRESS` (owner BCL_RECORD),
    `TEMP_CLOSED`, `CLOSED` (none terminal). Transitions (stage.action -> stage, permission): `NEW.progress` ->
    IN_PROGRESS, `NEW.temp_close` / `IN_PROGRESS.temp_close` -> TEMP_CLOSED (BCL_STATUS_UPDATE), `TEMP_CLOSED.resume` ->
    IN_PROGRESS (BCL_STATUS_UPDATE), `NEW.close` / `IN_PROGRESS.close` / `TEMP_CLOSED.close` -> CLOSED (BCL_CLOSE),
    `CLOSED.reopen` -> IN_PROGRESS (BCL_REOPEN, reason list `BCL_REOPEN_REASON`). None is generic. There is no
    transition back to NEW: the status engine refuses a status of phase NEW once the claim has left it, or keeps the
    stage (CL1-B decides).
  - Template `BCL_LOSS_ADVICE` v1 with the placeholders `claimNo`, `assuredName`, `insurerName`, `policyNo`, `arn`,
    `policyYear`, `lossDate`, `lossPlace`, `lossNature`, `lossDescription`, `currency`, `initialReserve`,
    `adjusterName`, `insurerClaimNos`, `handlerName`.
  - Retention rule record type `BROKER_CLAIM` (not `BrokerClaim`: the rule codes are upper snake), statuses `CLOSED`,
    10 years online + 5 archive (purge after 15, p.41), action REVIEW.
- **Migration `V1021__brokerclaims_claim.sql`.** `bcl_claim` with every column of 5.1 (NOT NULL: company, claim no.,
  handler, source, ARN, policy year, currency, loss date, reported date, phase, the two override flags; check reported
  date >= loss date; unique (company, claim no.) and authorization code), `bcl_claim_location` (unique claim + item
  no.; `location_key` varchar(400) as on `acc_risk_item`), `bcl_insurer_claim` (company, claim, insurer, share_pct
  numeric(9,4) as on `ops_invoice_share`, insurer claim no., reported to insurer on, reserve, settled, adjuster; unique
  claim + insurer + coalesce(number, '') so a line may wait for its number; index company + insurer + number),
  `bcl_status_history` (insert-only).
- **Domain `brokerclaims.domain`.** `Claim` (JPA entity name `BrokerClaim`, since `claims.domain.Claim` exists;
  constructor `Claim(companyId, claimNo, Claim.Origin(source, handler, unitCode, branchId, legacyRef), CoverSnapshot,
  LossDetails)` starts in phase NEW without status; `assignTo(handler, unitCode)`; `isClosed()`), repository
  `BrokerClaimRepository` (not `ClaimRepository`: the insurer-side module owns the bean `claimRepository`) with
  `findByIdAndCompanyId`, `findByCompanyIdAndClaimNo`, `findByCompanyIdAndCoverArnAndCoverPolicyYearOrderByIdDesc`.
  Embeddables with protected constructors and getters only: `CoverSnapshot` (cover columns **and** the premium check /
  authorization columns, CL1-A), `LossDetails` (loss **and** claimant columns, CL1-A), `ClaimProgress` (progress
  columns, CL1-B; package factory `newClaim()`). Enums `ClaimPhase` (`isOutstanding()`, `stageCode()`), `ClaimSource`,
  `ClaimClosureKind`, `ClaimPremiumStatus`. `ClaimLovAttribute` / `ClaimLovAttributeRepository` (read for all waves;
  CL1-B adds maintenance). `ClaimCodes`: entity type `BrokerClaim`, workflow, LOV, attribute, parameter, template,
  number prefixes (`BCL`, `CAC`), module and retention constants.
- **Event `ClaimStatusChanged`** (`brokerclaims.domain`): `(claimId, companyId, claimNo, fromStatus, toStatus,
  fromPhase, toPhase, changedBy, changedAt)` with `phaseChanged()`, `entered(ClaimPhase)`, `enteredStatus(code)`.
  Published by CL1-B's status engine inside the transaction after the history row, also for the first status at
  recording (from = null), a closure by settlement type and a reopen. CL1-A listens for the feed and notifications.
- **Report.** `ReportCategory.CLAIMS_HANDLING` ("Claims Handling"); `ReportMetadata.claimsHandling(code, title,
  description, parameters)` (view BCL_REPORT_VIEW, export BCL_REPORT_EXPORT, archived).
- **Crons.** `brokerverse.jobs.bcl-premium-recheck-cron` (05:30 PHT), `bcl-follow-up-due-cron` and
  `bcl-ageing-alerts-cron` (06:00 PHT) in `application.yml` and `CONFIGURATION.md`; the jobs come with CL1-A / CL1-B.
- **Demo `V1920__demo_brokerclaims_users.sql`.** Users of 7.3 (password `Brokerverse@2026`); `clmbranch` has home
  branch CEB; handler register rows for the five users with a unit.
- **Frontend.** `features/brokerclaims/module.ts` (`brokerClaimsModule`, first in group Claims & Insurance) declares
  every route of section 11: `/claims-handling` (Claims Home, BCL_VIEW, live landing), `/worklist` (BCL_VIEW),
  `/new` (BCL_RECORD), `/:id` (hidden, BCL_VIEW), `/covers` (BCL_COVER_VIEW), `/diary` (BCL_VIEW), `/location-refs`
  (BCL_LOCATION_REF_MAINTAIN), `/reports` (BCL_REPORT_VIEW or BCL_DATA_EXTRACT), `/setup` (BCL_SETUP). Placeholder pages
  per owning wave: CL1-A `record/RecordClaimPage.tsx`, `record/ClaimPage.tsx`, `cover/CoverLookupPage.tsx`,
  `location/LocationRefsPage.tsx`; CL1-B `home/ClaimsHomePage.tsx`, `worklist/WorklistPage.tsx`, `diary/DiaryPage.tsx`,
  `setup/ClaimsSetupPage.tsx`, `reports/ClaimsReportsPage.tsx`; shared `ClaimsPlaceholder.tsx` (`CLAIMS_SECTION`
  breadcrumb). Help section `BROKER_CLAIMS_HELP` (id `brokerclaims`) in `features/brokerclaims/help.ts`, registered
  before Underwriting, with one entry per menu route.
- **Flyway left.** Schema V1022 (CL1-A), V1023-V1024 (CL1-B), V1025 held (CLQ14), V1026-V1029 free; demo V1921
  (CL1-A), V1922-V1929 free.

## 18. CL1-A claim record and insurers: as built

What wave CL1-A built on the CL0 foundation, and the contracts CL1-B and CL2 use. Sub-packages `cover`, `claim`,
`insurer`, `location`, `feed` and `demo` of `brokerclaims`; frontend `features/brokerclaims/{record,cover,insurer,
location}`.

- **Cover** (`cover.service.CoverService`, read-only). The cover is the account (ARN) and a **policy year of its term,
  1 = first year** (the unit of `opsledger` invoices and of `acc_account_policy`), not the calendar year of the FRS
  test data; the period of year n is `period_from + (n-1)` years to the next anniversary or `period_to`. Search by
  ARN / assured (`AccountQueryService.search`) or policy number (read-only SQL on `acc_account_policy`), at least 3
  characters (`BCL_SEARCH_TOO_SHORT`), no branch filter. `snapshot(account, year, lossDate)` builds `CoverSnapshot`
  (policy number of the year, version = booked endorsements of the year effective on or before the loss date,
  sales stamp, invoicing branch of the year's first invoice, currency = account currency else
  `BCL_DEFAULT_CURRENCY`). `premium(arn, year)` runs `PremiumRule` (pure) over the ledger invoices. `shares(account,
  year)` proposes the insurers from the first live invoice's `OpsInvoiceShare`s, else the lead insurer at 100 %.
  Opening a cover (`GET /covers/{arn}`) writes an `OPEN` audit entry on the account.
- **Record** (`claim.service.ClaimRecordingService.record(companyId, NewClaim)`). Validates the cover, loss nature,
  claim type and description, the LOVs, the dates (`LossDetails` invariants), the first status (`NEW_COMPLETE_DOCS` /
  `NEW_INCOMPLETE_DOCS`, default `NEW_INCOMPLETE_DOCS`), an insurer-reported claim with an insurer claim number;
  refuses `MIGRATED` (CLQ14). A loss date outside the policy year is `BCL_LOSS_OUTSIDE_COVER` unless
  `confirmOutsidePeriod`. Number `BCL-<yyyy>-nnnnnn`; handler = current user, unit and branch from `bcl_handler` and
  `sec_user.home_branch_id` (`HandlerDirectory`); locations and insurer lines linked; premium checked (alert
  `BCL_UNPAID_PREMIUM_CLAIM:<id>`); workflow case `BCL_CLAIM` started in `NEW` (assigned to the recorder); audit
  `CREATE`; in-app notice `BCL_CLAIM_ASSIGNED` to the account officer (FR-CL-011).
- **Contract to CL1-B: `claim.service.ClaimRecorded`** `(claimId, companyId, claimNo, initialStatus, recordedBy,
  recordedAt)`, published inside the recording transaction after everything is saved. The status engine listens
  (`@EventListener`), sets the first status and the next follow-up date and publishes `ClaimStatusChanged`. Until
  CL1-B lands a recorded claim has phase `NEW` and no status.
- **Premium and authorization** (`claim.service.PremiumCheckService`): `check(claim)`, `recheck(companyId, id)`,
  `authorize(companyId, id, evidenceAttachmentId)` (`CAC-<yyyy>-nnnnnn`, once; `BCL_PREMIUM_UNPAID` "The premium of
  <ARN> <year> is not fully paid. The authorization code cannot be generated"; DIRECT_PAYMENT under
  `BCL_AUTH_DP_POLICY` ALLOW / CONFIRM (evidence = an attachment of the claim, `BCL_DP_EVIDENCE_REQUIRED`) / BLOCK).
  The handler gets an in-app notice when a blocking check becomes PAID. `ClaimOperationsSync` +
  `OperationsListener` (after commit, own transaction): `InvoiceMovementPosted` / `InvoiceFlagChanged` re-check the
  open claims of the invoice's ARN and year; `RemittanceStatusChanged` to FULLY_REMITTED notifies the handlers of
  claims in an `awaiting_premium_remittance` status (`BCL_PREMIUM_REMITTED`); `OpsInvoiceBooked` of an endorsement
  re-checks and notifies `BCL_NEWER_COVER_VERSION`. Job `BCL_PREMIUM_RECHECK` (`PremiumRecheckJob`, cron
  `bcl-premium-recheck-cron`) re-checks open unauthorised claims.
- **Details** (`ClaimDetailsService`): `amendLoss` (BCL_RECORD), `correctReportedDate` (BCL_STATUS_UPDATE, reason
  from `BCL_OVERRIDE_REASON`, refused on a closed claim), `overrideClaimant` (BCL_CLAIMANT_OVERRIDE),
  `refreshCover` (policy number, sum insured, sales stamp, branch; returns the changes) and `useLatestVersion`.
  Every change writes an audit entry with old and new values; CL1-B's `bcl_claim_event` timeline may read them.
- **Locations** (`location.service.ClaimLocationService`, entity `ClaimLocation` on `bcl_claim_location`): link /
  describe / remove locations of the claim's own cover only (`BCL_LOCATION_NOT_ON_COVER`, `BCL_LOCATION_LINKED`).
  **Insurer location references** (`LocationRefService`, entity `LocationRef` on `bcl_location_ref`, V1022): one open
  reference per location and insurer; a new one end-dates the open one the day before (`BCL_EFFECTIVE_DATE` "The
  effective date must be after <dd-MMM-yyyy>"); `validOn(company, arn, date)` feeds every claim location.
- **Insurers** (`insurer.service.InsurerClaimService`, entity `InsurerClaim` on `bcl_insurer_claim`): `add`, `number`
  (a line that already has a number keeps it; the new number becomes a further line of the insurer),
  `changeShare`, `amendReserve` (`InsurerReserveChange` history on `bcl_reserve_change`, V1022; no journal), `assignAdjuster` (per insurer line).
  Duplicates on the claim are refused (`BCL_INSURER_CLAIM_NO_DUPLICATE`); a number found on another claim is
  `BCL_INSURER_CLAIM_NO_REUSED` until confirmed, then raises the alert. **For CL1-B:** `InsurerClaim.settled(amount)`
  records the settled amount of a line; the claim-level adjuster (`ClaimProgress.adjusterCode`) stays CL1-B's.
- **Insurer updates** (`InsurerUpdateService`, entity `InsurerUpdate`, V1022, insert-only; allowed on closed claims;
  a correction refers to the update it corrects) and the **loss advice** (`LossAdviceService` + `LossAdviceDocument`):
  one advice per insurer from template `BCL_LOSS_ADVICE` (PDF on the company letterhead), e-mailed through
  `MessageService` (purpose `BCL_LOSS_ADVICE`, record link `BrokerClaim`), stored as document type `CLAIM_REPORT` on
  the claim (nominated name) and linked to the account and the client (decision D3).
- **Feed** (`feed.service.InAppClaimsFeed implements ClaimsFeed`): feed `CLAIMS_SPECIAL_REMIT`, one item per
  non-cancelled invoice of the claim's ARN and year not FULLY_REMITTED / NOT_APPLICABLE, for open claims whose
  status carries `awaiting_premium_remittance` and `status_since >= since`; fields `claimNo, arn, policyYear, status,
  handler, statusSince`. The remittance module now refuses a CLAIMS-condition special remittance without such a claim
  (the remittance test uses another condition for its generic case; `ClaimsFeedIT` covers the claims case).
- **Providers**: `BrokerClaimClientRecords` (client 360), `BrokerClaimRetentionProvider` (record type
  `BROKER_CLAIM`, statuses of the rule are claim phases, last activity = `updated_at` else `created_at`).
- **Bulk handlers**: `BCL_INSURER_UPDATE` (claim no., or insurer + insurer claim no.; update date, source, reference,
  remarks; handler notified), `BCL_INSURER_CLAIM_NO` (claim no., insurer, number, reported on) and
  `BCL_LOCATION_REF` (ARN, item no., insurer, reference, effective from). Every row is validated before commit; the
  bulk framework commits the valid rows only, so "refuse the whole file" is the reviewer's decision on the review
  screen (FRS v1.1 note).
- **API** (`/api/v1/broker-claims`, every call takes `companyId`): `GET covers?by&q`, `GET covers/{arn}`,
  `GET covers/{arn}/claim-draft?policyYear&lossDate`; `POST /` (record), `GET /{id}`, `GET /search?q`,
  `PUT /{id}/loss`, `POST /{id}/reported-date|claimant|refresh-cover|latest-version|premium-check|authorize`;
  `GET|POST /{id}/locations`, `PUT|DELETE /{id}/locations/{itemNo}`; `GET|POST /{id}/insurers`,
  `GET /{id}/insurers/reserve-history`, `POST /{id}/insurers/{line}/number|reserve|adjuster`, `PUT .../share`;
  `GET|POST /{id}/updates`; `GET|POST /{id}/loss-advice`; `GET|POST location-refs`, `GET location-refs/by-cover/{arn}`.
- **Screens**: Record Claim (cover search, cover card with premium panel, locations picker, loss, insurers,
  confirmation dialog), Cover Lookup (read-only tabs), Insurer Location References, and the claim record
  `ClaimPage` with the summary card and flags, `WorkflowPanel`, actions (Generate Authorization Code, Send Loss
  Advice, Refresh Cover Data, Use Latest Version), the special remittance link when the status awaits the premium
  remittance, and tabs Details, Locations, Insurers & Updates, Reserve & Settlement, Documents. **CL1-B** adds its
  status actions to `record/ClaimActions.tsx` and its Diary / History tabs to `record/ClaimPage.tsx` (`TABS`).
- **Demo**: V1921 insurer location references of the booking demo property covers; `BrokerClaimsDemoData`
  (`@Order(130)`, demo profile, idempotent, `load()` for CL1-B to extend) records a motor claim (authorised when
  paid), a property typhoon claim with location, insurer number, update and reserve, a direct-payment claim and an
  insurer-reported claim.
- **Parked / open**: meaning of the authorization code (CLQ01); claims mailbox of the insurer (the placement
  mailboxes are proposed, CLQ11 / CLQ22); location lists per endorsement (CLQ02); the remittance special form does not
  read `?invoiceNo=&condition=CLAIMS` yet (UI ask to the remittance owner, section 12.2).
