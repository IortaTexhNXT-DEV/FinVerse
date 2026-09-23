# Reinsurance (treaties, cessions, FAC, claims recoveries, statements of account)

Package `com.iortatechnxt.finverse.reinsurance`, frontend `src/features/reinsurance`, migrations
`V300` (schema, event types, alert codes, permission) and `V930` (demo parties and posting rules),
demo loader `reinsurance.demo.ReinsuranceDemoData` (`@Profile("demo")`, `@Order(30)`, idempotent).

All reinsurance amounts are accounted in the **company base currency**. A policy in a foreign
currency is split in its own currency and converted at the cession's rate; treaty limits are in base
currency and converted into the policy currency before the split.

## 1. Treaty model

| Element | Content |
|---|---|
| Treaty | company, code, name, type (`QUOTA_SHARE`, `SURPLUS`, `XOL`), line of business, underwriting year, period, currency (= base currency), optional broker (`RI_BROKER` party) |
| Terms | levy / premium tax %, interest on reserves % p.a., outstanding loss reserve retained %, statement frequency (quarterly) |
| Quota share | QS % and an optional limit per risk (the "line" when there is no surplus) |
| Surplus | retention (one line) and number of lines |
| Excess of loss | layers: layer no, priority (deductible), limit, minimum and deposit premium (MDP), reinstatements; aggregate limit = limit x (1 + reinstatements) |
| Participants | reinsurer (`REINSURER` party), share %, commission %, profit commission %, premium reserve % |

Rules: shares add up to 100 %; one active treaty per type, line of business and underwriting year;
maker-checker (`REINSURANCE_MAINTAIN` / `REINSURANCE_AUTHORIZE`, never the maker). `RI_BROKER` was
added to the shared `PartyType` (REINSURER sub-ledger); the broker can be paid or can pay.

## 2. Cession engine

### Algorithm (per risk, company share of the sum insured)

1. The **line** L is the surplus retention, else the QS limit per risk, else unlimited.
2. The first layer is `min(SI, L)`: the quota share takes QS % of it and the company keeps the rest
   (retention first, then QS %).
3. The surplus takes `min(lines x L, SI - first layer)`.
4. Anything left is the **facultative requirement** (a FAC placement is opened).
5. Premium follows the sum insured (`premium x layer SI / SI`); the retention absorbs rounding.
   Each treaty layer is shared among its participants by share %; commission = premium x
   commission %.

### Worked example

Fire risk, SI 120,000,000, premium 240,000; FIRE-QS 40 % and FIRE-SP with retention 25,000,000
and 3 lines.

| Layer | Sum insured | Premium |
|---|---:|---:|
| Quota share (40 % x 25M) | 10,000,000 | 20,000 |
| Retention (25M - 10M) | 15,000,000 | 30,000 |
| Surplus min(3 x 25M, 120M - 25M) | 75,000,000 | 150,000 |
| FAC (remainder) | 20,000,000 | 40,000 |
| **Total** | **120,000,000** | **240,000** |

If the surplus has two participants at 60 % / 40 % with 30 % commission, the first is ceded
90,000 premium less 27,000 commission (net due 63,000), the second 60,000 less 18,000 (42,000).

### Transactions

| Transaction | Basis | Treatment |
|---|---|---|
| Original issue, renewal | `FULL` | split risk by risk as above |
| Endorsement with premium, refund | `PRO_RATA` | the proportions of the full allocation in force at the effective date; the FAC share goes to the placed participants, an unplaced FAC share increases the provisional requirement |
| Cancellation | `PRO_RATA` | negative premium, reverses the cession in the same proportions |
| NIL endorsement | `NONE` | nothing ceded |

Cessions are idempotent per transaction (policy + endorsement no.): ceding again returns the
existing cession. They run on demand per policy (`POST /cessions/policies/{policyId}`, which also
cedes any earlier transaction not yet ceded first), in a batch **RI allocation run** for a period
(`/allocation/preview`, `/allocation/runs`, one transaction per database transaction, failures are
reported and do not stop the run) and as the scheduled job `RI_ALLOCATION`
(`finverse.jobs.ri-allocation-cron`, off by default). A sum insured above treaty capacity raises the
`RI_TREATY_CAPACITY` alert.

### Facultative placements

`PROVISIONAL` (opened by the cession, participants assigned) -> `PENDING_APPROVAL` (submitted) ->
`PLACED` (checker; the placed shares are fixed and ceded) -> `CLOSED`; a rejected slip returns to
`PROVISIONAL`. The share not placed stays with the company. Slips unplaced for more than N days
(alert parameter, 30 in V300) raise `RI_FAC_UNPLACED`.

## 3. Claims

`ClaimRecoveryListener` implements the kernel `ClaimMovementListener`; the module does not depend on
the claims module. It is idempotent on the movement reference (a reference already recorded is
ignored).

* Proportional shares use the cession in force at the loss date, with SI-weighted percentages at
  policy level (the kernel movement has no risk id).
* `RESERVE_CHANGE`: reinsurers' share of the reserve change.
* `PAYMENT`: recovery due from each participant (a DEBIT open item per participant).
* `RECOVERY` (salvage, subrogation): a negative recovery and a lower net retained loss; the reserve side follows the reserve change the claims module emits.
* Excess of loss: the cumulative net retained loss of the claim (after proportional shares) above
  the layer priority, capped at the layer limit and the aggregate limit, on the XOL programme of the
  loss year.

`ReserveShareView` implements `ClaimReinsuranceView` (reinsurers' share of outstanding claims per
claim); `CessionFiguresView` implements `PolicyReinsuranceView` (ceded premium, commission and FAC
per policy transaction). `POST /claims/catch-up` replays the movements of an optional
`ClaimsExperienceView` (if the claims module provides one), skipping those already processed.

## 4. Accounting (demo rules, FVI)

| Event | Source reference | Dr | Cr | Open item |
|---|---|---|---|---|
| `RI_PREMIUM_CEDED` (treaty cession) | `RI:CES:<cession>:T<id>:<party>` | 4200 premium ceded (`CEDED_PREMIUM`) | 2201 due to reinsurers (`NET_DUE`, party), 4400 commission (`RI_COMMISSION`) | CREDIT `REINSURANCE_PREMIUM` (net due) |
| `RI_PREMIUM_CEDED` (FAC placed) | `RI:FAC:<placement>:<party>` | as above | as above | as above |
| `RI_PREMIUM_CEDED` (XOL MDP / 4, on SOA approval) | `RI:SOA:<soa>:PREM` | as above | as above | as above |
| Refund / cancellation | same events, negative amounts | reverse | reverse | DEBIT `REINSURANCE_PREMIUM_RETURN` |
| `RI_RESERVE_SHARE` | `RI:CLM:<movement>` | 1302 RI share of claims reserves | 5300 RI share of claims | – |
| `RI_CLAIM_RECOVERY` | `RI:CLM:<movement>:<contract>:<party>` | 1205 recoverable (party) | 5300 RI share of claims | DEBIT `REINSURANCE_RECOVERY` (salvage: CREDIT `..._RETURN`) |
| `RI_SOA_ADJUSTMENT` (new, SOA approval) | `RI:SOA:<soa>` | 2201 (levy, premium reserve, loss reserve); 4200 (interest) | 2507 premium tax (levy), 2202 funds held (reserves); 2201 (interest) | DEBIT `REINSURANCE_SOA_ADJUSTMENT` |
| `RI_SETTLEMENT_PAYMENT` | `RI:SOA:<soa>:SETTLE` | 2201 (party) | bank | DEBIT `REINSURANCE_PAYMENT` |
| `RI_SETTLEMENT_RECEIPT` | `RI:SOA:<soa>:SETTLE` | bank | 1205 (party) | CREDIT `REINSURANCE_RECEIPT` |
| `RI_BALANCE_OFFSET` (new, settlement) | `RI:SOA:<soa>:OFFSET` | 2201 (party) | 1205 (party) | – |

The accounting engine is idempotent on the source reference, so a retry never posts twice. Posting
dates are the RI accounting dates (approval date of the policy transaction, placement date,
movement date, statement or settlement date).

## 5. Statements of account

Quarterly, per treaty and participant (`SOA-<company>-<year>-nnnnnn`), maker-checker:
`PENDING_APPROVAL` (generated, may be regenerated) -> `APPROVED` (posts `RI_SOA_ADJUSTMENT` and,
for XOL, the MDP instalment) -> `SETTLED`.

| Particulars | Income (reinsurer) | Outgo (reinsurer) |
|---|---:|---:|
| Premium ceded | x | |
| Commission | | x |
| Premium tax / levy (premium x levy %) | | x |
| Losses paid | | x |
| Recoveries (salvage / subrogation) | x | |
| Premium reserve retained (premium x reserve %) | | x |
| Premium reserve released (retained 4 quarters ago) | x | |
| Interest on reserves (reserves held x % / 4) | x | |
| O/S loss reserve retained (share of O/S x loss reserve %) | | x |
| O/S loss reserve released (previous quarter) | x | |
| **Balance** (placed on the smaller side) | | |
| **Total** (both sides equal) | | |

The balance shows as "Balance due to reinsurer" or "Balance due from reinsurer" with the amount in
words (`AmountInWords`). Settlement takes the statement's outstanding open items (its cessions,
recoveries, adjustments and XOL premium), nets the premium side against the recovery side, pays or
receives the net through the chosen bank account, sets off the smaller side with
`RI_BALANCE_OFFSET` and matches all the items.

## 6. Approvals and alerts

`ReinsuranceApprovalSource` lists treaties, FAC slips and statements pending approval in the
approvals inbox. Alert checks: `RI_FAC_UNPLACED` (placement not placed after N days) and
`RI_TREATY_CAPACITY` (raised when a cession exceeds treaty capacity).

## 7. Reports (category REINSURANCE)

| Code | Title |
|---|---|
| PGIR0637 | Reinsurance Premium Register (per risk: retention, QS, surplus, FAC) |
| PGIR0692 | FAC Premium Register |
| PGIR0693 | Policies Pending for FAC Closing |
| PGIR0638 | Reinsurance Claims Paid Register |
| PGIR0696 | Treaty Claims Paid |
| PGIR0639 | Reinsurance Claims Outstanding Register |
| RI-SOA | Statement of Account for Reinsurer (treatyYear, treatyCode, quarter, statementDate, reinsurerCode) |
| RISK-PROFILE | Risk Profile by SI band (uwYearFrom, uwYearTo, fromDate, toDate) |
| RI-BDX | Treaty Bordereau, premium or claims (treatyCode, bordereau) |
| RI-BAL | Reinsurer Balance / Ageing (asOfDate) |

All export to PDF, XLSX and CSV.

## 8. Ports

| Port | Owner | Implementation |
|---|---|---|
| `underwriting.service.PolicyReinsuranceView` | underwriting | `CessionFiguresView` |
| `insurance.ClaimMovementListener` | kernel | `ClaimRecoveryListener` |
| `insurance.ClaimReinsuranceView` | kernel | `ReserveShareView` |
| `insurance.ClaimsExperienceView` | kernel (claims) | consumed, optional (`ObjectProvider`) |

## 9. Demo data (FY2026, FVI)

Treaties FIRE-QS-26 (40 %) and FIRE-SP-26 (3 lines), MAR-QS-26, ENG-SP-26 and MOT-XL-26 with
reinsurers R-0001, R-0003, R-0004, R-0005 and broker RB-0001. The loader runs the allocation for the
demo policies, places part of the FAC slips (others stay pending), approves the Q1 and Q2 statements,
settles one of them and catches up claims when a `ClaimsExperienceView` is present.

## 10. Open points

* Profit commission % is stored but not computed; reinstatement premiums are not computed.
* Claims are apportioned at policy level (no risk id on the kernel movement).
