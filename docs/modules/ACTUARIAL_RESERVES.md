# Actuarial Reserves (period-end technical provisions)

Package `com.iortatechnxt.brokerverse.reserves`; UI section **Actuarial Reserves**
(`frontend/src/features/reserves`). Migrations `V420` (schema, events, job parameter) and `V421`
(permission); demo `V940` (accounts and rules) and the start-up runner `reserves.demo.ReservesDemoData`
(`demo` profile, `@Order(90)`, idempotent).

The module values, once a month per company, the technical reserves of a non-life insurer and books
their **movement** through the accounting engine:

| Reserve | Source | Gross | Reinsurers' share |
|---|---|---|---|
| UPR | premium transactions (underwriting) | company net premium × unearned units / total units | treaty + FAC premium × the same fraction |
| DAC / UCR | same transactions | commission × unearned fraction (DAC, asset) | RI commission × unearned fraction (UCR, liability) |
| OSLR | `ClaimsExperienceView.outstanding` | Σ outstanding of open claims | `ClaimReinsuranceView` |
| IBNR | rate or chain-ladder per line of business | see below | IBNR × ceded share of earned premium |
| ULAE | parameter | ULAE % × (OSLR + IBNR) gross | none |
| MfAD | parameter | MfAD % × (OSLR + IBNR) gross | MfAD % × (OSLR + IBNR) RI share |
| PDR | liability adequacy test | deficiency per line of business | none |
| Takaful surplus | PGIBR074 rule | payable to participants of expiring policies | – |

Reserves never depend on the claims or reinsurance modules: the kernel views
(`insurance.ClaimsExperienceView`, `insurance.ClaimReinsuranceView`) and the underwriting port
`PolicyReinsuranceView` are injected optionally (`ReservePorts`, `UnderwritingPorts`). Without them
OSLR, chain-ladder IBNR and every reinsurers' share are zero and the run remarks say so.

## Formulas

### UPR, DAC and UCR (PGIBR072)

Every approved premium transaction (policy issue, renewal, additional / refund / cancellation
endorsement; NIL endorsements are skipped) approved on or before the valuation date V is earned over
its **cover period** (`PolicyQueryService.coverPeriods`): the original issue and renewals over their own
period, other endorsements from their effective date to the end of the policy period in force.

The earning basis is the product's `uprBasis`:

| Basis | Units | Earned units at V | Unearned |
|---|---|---|---|
| `DAYS_365` (1/365, default, spec PGIBR072) | days of cover, both dates inclusive | days from the start up to and including V | total − earned = days after V |
| `TWENTY_FOURTHS` (1/24) | 2 × cover months (a started month counts) | 2e − 1 in the e-th month of cover | total − earned |
| `EIGHTHS` (1/8) | 2 × cover quarters | 2e − 1 in the e-th calendar quarter of cover | total − earned |

Nothing is earned before the cover starts; a cover whose end precedes its start (zero-day) has no
units and no UPR. A leap year simply has 366 days of cover.

```
fraction      = unearned units / total units
UPR gross     = company net premium (after discount and loading, gross of RI) × fraction
UPR RI share  = (treaty premium + FAC premium) × fraction
DAC           = intermediary commission × fraction
UCR           = (treaty premium × treaty commission % + FAC premium × FAC commission %) × fraction
```

All amounts are converted to base currency at the transaction's exchange rate and rounded to cents.

*Worked example.* Annual policy 1 Jan – 31 Dec 2026, premium 36 500, commission 5 475, treaty
premium 10 950 (25 % commission). At 31 March: 90 days earned, 275 unearned →
UPR = 36 500 × 275 / 365 = 27 500; DAC = 4 125; RI UPR = 8 250; UCR = 2 737.50 × 275 / 365 = 2 062.50.
A pro-rata cancellation effective 1 October returns 9 200 over 92 days; from October the original's
remaining UPR (6 100 at 31 October) and the cancellation's negative UPR (−6 100) cancel out.

### IBNR (PGIBR079 and chain-ladder)

The method is chosen per line of business in the reserve parameters.

**Rate method.** `IBNR = base × rate %`, where base = earned premium (gross of reinsurance) of the
twelve months ending at V:

```
earned premium(open, close] = written in (open, close] + UPR at open − UPR at close
ceded share                 = earned ceded premium / earned premium
IBNR RI portion             = IBNR × ceded share
```

*Example.* Earned premium 200 000, rate 5 %, earned ceded premium 50 000: IBNR = 10 000, RI = 2 500.

**Chain-ladder.** The paid (payments − recoveries) or incurred (paid + reserve changes) triangle is
built from `ClaimsExperienceView.movements` by accident period (loss date) and development age
(movement date), by year or quarter, for the configured number of accident periods up to V.

```
f(k)        = Σ C(i, k+1) / Σ C(i, k)   over accident periods observed at both ages (1 if Σ = 0)
CDF(i)      = Π f(k) from the latest age of i to the last age (no tail factor)
ultimate(i) = latest C(i) × CDF(i)
IBNR(i)     = ultimate(i) − incurred to date(i)
IBNR(line)  = max(0, Σ IBNR(i))
```

*Worked example.* AY1 100, 150, 175; AY2 110, 168; AY3 120 → f(0) = 318 / 210 = 1.514286,
f(1) = 175 / 150 = 1.166667; ultimates 175, 196, 212 (120 × 1.766668).

The line's IBNR is allocated to its branches / products / channels in proportion to their earned
premium (equally over the units with open claims when nothing was earned); the RI portion uses each
unit's ceded share.

### OSLR (PGIBR080)

`OSLR = Σ (approved estimate − paid)` of open claims at V, company share in base currency, grouped by
branch, line of business, product and channel of the policy; RI share from `ClaimReinsuranceView`.

### ULAE, MfAD and premium deficiency

```
ULAE  = ULAE % × (OSLR + IBNR) gross
MfAD  = MfAD % × (OSLR + IBNR), gross and RI share
LAT per line of business:
  expected claims = net UPR × expected loss ratio %
  available       = net UPR − net DAC  (net DAC = DAC − UCR)
  PDR             = max(0, expected claims − available), allocated to units by net UPR
```

### Takaful surplus / Mudharabah (PGIBR074, BrokerVerse rule)

Optional per company (**Reserve Parameters → Takaful surplus**, maker-checker). For each takaful
product policy expiring in the valuation month (amounts of the expiring period):

```
applicable = gross − discount + loading − commission − claims (incurred, from PolicyClaimsView)
surplus    = applicable − retakaful (treaty + FAC premium ceded)
payable before tax = surplus × participants' share %   (0 when surplus ≤ 0)
tax        = payable before tax × tax %
payable    = payable before tax − tax
```

## Accounting

Posting unit = branch × line of business; value date = V; source module `RESERVES`; source reference
`RSV:<run>:P:<event>:<branch>:<line>` (reversal `…:R:…`). Each amount component is the signed
movement **closing balance of this run − balance of the previous posted run**; a negative amount posts
to the opposite side. Demo rules (V901 / V940):

| Event | Component | Debit | Credit |
|---|---|---|---|
| `UPR_PROVISION` | `UPR_CHANGE` | 4300 Decrease/(increase) in UPR | 2101 Reserve for unearned premiums |
| | `RI_UPR_CHANGE` | 1301 Reinsurers' share of UPR | 4300 |
| `DAC_PROVISION` | `DAC_CHANGE` | 1400 Deferred acquisition costs | 5500 Change in DAC |
| | `DRC_CHANGE` (UCR) | 4400 Commission income | 2400 Deferred reinsurance commissions |
| `IBNR_PROVISION` | `IBNR_CHANGE` | 5200 Change in claims reserves | 2103 Claims reserve – IBNR |
| | `RI_IBNR_CHANGE` | 1303 Reinsurers' share of IBNR | 5300 Reinsurers' share of claims |
| `CLAIM_MARGIN_PROVISION` (V420) | `ULAE_CHANGE` | 5200 | 2105 Claims reserve – ULAE |
| | `MFAD_CHANGE` | 5200 | 2106 Claims reserve – MfAD |
| | `RI_MFAD_CHANGE` | 1304 Reinsurers' share of risk margin | 5300 |
| `PREMIUM_DEFICIENCY_PROVISION` (V420) | `PDR_CHANGE` | 4300 | 2104 Premium deficiency reserve |
| `TAKAFUL_SURPLUS` | `SURPLUS` (flow of the month, cost centre of the takaful settings) | 5613 | 2502 |

OSLR is **not** posted by reserving: claims books it (`CLAIM_RESERVE`, 5200 / 2102) and reinsurance
its share (`RI_RESERVE_SHARE`, 1302 / 5300). Because every posting is a movement against the previous
posted run, the ledger balance of each reserve account equals the reserve of the latest posted run
(tested in `ReservesIT`).

## Valuation run life cycle

```
create (preview) ─► PREVIEW ─submit─► PENDING_APPROVAL ─approve─► APPROVED ─post─► POSTED
                     ▲   │ recalculate          │ reject (reason)                 │
                     └───┘◄─────────────────────┘                                 │ cancel (reason)
   any status except CANCELLED ── cancel ─► CANCELLED ◄──────────────────────────┘ (reverses journals)
```

- **Preview** (`RESERVE_PREPARE`): calculates and stores the lines (per reserve, branch, line,
  product, channel), the policy-level UPR (transactions still unearned or approved in the month) and
  the takaful lines. The valuation date is the month end of the date entered. One live run per
  company and month.
- **Approve / reject / post / cancel** (`PERIOD_END_RUN`): the preparer (submitter) can never approve.
  Posting is idempotent (posting a posted run returns it) and in date order (not while a later run is
  posted). Only the latest posted run can be cancelled; the reversal is dated the valuation date
  while its period accepts postings, else the first day of the next period (which must be open).
- **Approval inbox**: runs pending approval, reserve parameters and takaful settings pending
  authorization (`ReserveApprovalSource`).
- **Scheduled job** `RESERVE_VALUATION` (`ReserveValuationJob`, cron `brokerverse.jobs.reserve-valuation-cron`,
  default `-` = manual only): prepares and submits the previous month's run for the company codes in
  the system parameter `RESERVES_AUTO_RUN_COMPANIES`.
- **Period-end checklist**: control `RESERVE_VALUATION` "Actuarial reserves valued and posted" through
  the port `closing.service.PeriodEndCheckProvider` (not applicable for companies without reserve
  parameters or runs).

## Reserve parameters

Per company and line of business, effective dated (`rsv_parameter`, maker-checker; an authorized
record is never changed – add a record with a later effective date; deactivating falls back to the
previous record). Values: IBNR method, rate %, triangle basis (PAID / INCURRED), development period
(YEAR / QUARTER), number of accident periods (2–20), MfAD %, ULAE %, expected loss ratio %, treaty and
FAC reinsurance commission %. A line without authorized parameters is valued with zero IBNR, margins,
LAT and RI commission (listed in the run remarks).

## Reports (category *Processing & Reserves*, permission `REPORT_FINANCIAL`)

| Code | Title | Notes |
|---|---|---|
| PGIBR072 | UPR Summary Report | detail (per transaction, with units) or summary (per product); earned or unearned; gross, treaty and FAC premium and commission |
| PGIBR079 | IBNR Processing Report | base, rate (IBNR ÷ base), IBNR, RI portion, per unit or per branch and line |
| PGIBR080 | OSLR Processing Report | OSLR, RI share, net per unit |
| PGIBR074 | Surplus / Mudharabah Payment | runs in a date range, posted / unposted |
| RSV-SUMMARY | Technical Reserves Summary | UPR, DAC, UCR, OSLR, IBNR, ULAE, MfAD, PDR – gross, RI, net, current vs previous |
| RSV-TRIANGLE | IBNR Development Triangle | triangle, factors to ultimate, ultimate, incurred, IBNR; age-to-age factors in the notes |
| RSV-UPR-MOVE | UPR Movement | opening + written − earned = closing, gross and net, per line |

Processing reports (072 excepted, which is always calculated live) read the valuation run of the
month when there is one and otherwise calculate at the date.

## API (`/api/v1/reserves`)

| Method | Path | Permission |
|---|---|---|
| GET / POST / PUT | `/parameters`, `/parameters/{id}`; POST `/parameters/{id}/authorize`, `/deactivate` | MASTER_VIEW or RESERVE_PREPARE / MASTER_MAINTAIN / MASTER_AUTHORIZE |
| GET / PUT, POST `/authorize` | `/takaful-setting` | same |
| GET | `/runs?companyId`, `/runs/{id}`, `/runs/{id}/upr`, `/runs/{id}/takaful` | RESERVE_PREPARE or PERIOD_END_RUN |
| POST | `/runs`, `/runs/{id}/recalculate`, `/submit` | RESERVE_PREPARE |
| POST | `/runs/{id}/approve`, `/reject`, `/post`, `/cancel` | PERIOD_END_RUN |
| GET | `/summary?companyId&asOf`, `/triangles?companyId&businessLine&asOf[&basis&period&accidentPeriods]` | RESERVE_PREPARE, PERIOD_END_RUN or REPORT_FINANCIAL |

## Demo data

FVI: authorized parameters for FIRE (chain-ladder incurred / year), MOTOR (chain-ladder paid /
quarter), CASUALTY (chain-ladder incurred / quarter), MARINE, ENGG, PA, HEALTH (expected loss ratio
95 % → premium deficiency) and BONDS (rate); takaful enabled for `PA-IND` (70 % participants' share,
5 % tax, cost centre UW); runs January–August 2026 prepared by `accountant`, approved and posted by
`fmanager`; September 2026 submitted and pending approval.

## Known simplifications

- The start of a policy's original period is not stored once it is renewed (underwriting overwrites
  the header period); the issue date is used as the start of the original period.
- No tail factor in the chain-ladder; negative line IBNR is floored at zero.
- ULAE has no reinsurers' share; PDR is net only.
- The reinsurance commission rates for UCR come from the reserve parameters because
  `ReinsuranceFigures` carries ceded premium only.
