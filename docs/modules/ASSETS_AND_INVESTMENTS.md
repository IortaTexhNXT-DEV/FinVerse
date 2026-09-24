# Fixed Assets and Investments

Packages `com.iortatechnxt.brokerverse.fixedasset` and `com.iortatechnxt.brokerverse.investment`;
UI section **Assets & Investments** (`frontend/src/features/assets`). Every posting goes through the
accounting engine (`AccountingEventPublisher`); GL accounts come from the asset category or the
investment portfolio as account roles, and from the rules configured per company.

## Accounting events (V660, V670) and demo rules (V965)

| Event | Journal | Components | Demo rule (debit / credit) |
|---|---|---|---|
| `ASSET_ACQUISITION` | PAYMENT | COST | @ASSET / @SETTLEMENT (bank or supplier payable, party line) |
| `ASSET_TAKE_ON` | OPENING | COST, ACCUMULATED_DEPRECIATION, NET_BOOK_VALUE | @ASSET / @ACCUM_DEPRECIATION + 3500 |
| `ASSET_DEPRECIATION` | PROVISION | DEPRECIATION | @DEPRECIATION_EXPENSE / @ACCUM_DEPRECIATION |
| `ASSET_DISPOSAL` | RECEIPT | COST, ACCUMULATED_DEPRECIATION, PROCEEDS, GAIN, LOSS | @ACCUM_DEPRECIATION + @BANK + 5613 (loss) / @ASSET + 4700 (gain) |
| `ASSET_TRANSFER` | PROVISION | COST, ACCUMULATED_DEPRECIATION, NET_BOOK_VALUE | receiving branch @ASSET / @ACCUM_DEPRECIATION + 1605; sending branch the same amounts negated |
| `INVESTMENT_PURCHASE` | INVESTMENT | COST, PURCHASED_INTEREST, TOTAL | @INVESTMENT + @ACCRUED_INTEREST / @BANK |
| `INVESTMENT_TAKE_ON` | OPENING | CARRYING_AMOUNT, ACCRUED_INTEREST, TOTAL | @INVESTMENT + @ACCRUED_INTEREST / 3500 |
| `INVESTMENT_INTEREST_ACCRUAL` | INVESTMENT | INTEREST | @ACCRUED_INTEREST / @INTEREST_INCOME |
| `INVESTMENT_AMORTIZATION` | INVESTMENT | AMORTIZATION (negative = premium) | @INVESTMENT / @INTEREST_INCOME |
| `INVESTMENT_INTEREST_RECEIPT` | INVESTMENT | CASH, FINAL_TAX, ACCRUED_INTEREST, INCOME_ADJUSTMENT | @BANK + @INTEREST_INCOME (final tax) / @ACCRUED_INTEREST + @INTEREST_INCOME |
| `INVESTMENT_MATURITY`, `INVESTMENT_SALE` | INVESTMENT | PROCEEDS, FINAL_TAX, FV_RESERVE, CARRYING_AMOUNT, ACCRUED_INTEREST, REALIZED_GAIN | @BANK + @INTEREST_INCOME + @FAIR_VALUE / @INVESTMENT + @ACCRUED_INTEREST + @REALIZED_GAIN |
| `INVESTMENT_FAIR_VALUE` | REVALUATION | FAIR_VALUE_CHANGE | @INVESTMENT / @FAIR_VALUE (3400 reserve for FVOCI, 4504 for FVPL) |

## Fixed assets

- **Capitalization (maker-checker).** A maker registers the asset; a different user capitalizes it,
  which posts the acquisition on the capitalization date (default: acquisition date). An asset
  acquired before go-live is registered as a *take-on*: its opening accumulated depreciation and
  months charged are computed from the acquisition month to the take-on date (or entered) and posted
  as an OPENING journal against retained earnings.
- **Depreciation** (`DepreciationCalculator`): full-month convention (full month in the month of
  acquisition, none in the month of disposal); straight line = (cost − residual) / life months;
  declining balance = double-declining, net book value × 2 / life months; never below the residual
  value; the last month of the useful life charges the remainder. Charges are rounded half-even to
  centavos.
- **Monthly run**: preview, then post once per period (unique company + period; posting again
  returns the existing run). Months missed (late capitalization) are caught up. One journal per
  branch, category and cost centre, dated at the period end.
- **Disposal**: depreciation must be run up to the month before the disposal month; gain = proceeds −
  net book value (positive to 4700, negative to 5613 in the demo rules). When the run of the
  disposal month (or a later one) has already charged the asset, the disposal first reverses that
  charge (an `ASSET_DEPRECIATION` journal with a negative DEPRECIATION component, dated on the
  disposal date: Dr accumulated depreciation / Cr depreciation expense), so the gain or loss uses
  the net book value at the end of the previous month. The movement shows both journal numbers
  (reversal/disposal).
- **Inter-branch transfer**: two balanced journals through inter-branch clearing; the asset keeps
  depreciating at the receiving branch with status TRANSFERRED.

## Investments

- **Portfolios** fix the PFRS 9 classification (AMORTIZED_COST, FVOCI, FVPL) and the GL accounts.
- **Holdings** are approved by a checker, which posts the purchase (clean price + purchased accrued
  interest, computed from the coupon terms when not entered) or the take-on opening balance.
- **Day count** (`DayCountConvention`): `ACT_365` = actual days / 365; `THIRTY_360` = 30E/360 (day
  31 → 30, days = 360 × years + 30 × months + day difference, over 360). Both are additive, so
  accruing month by month equals the coupon of the whole period.
- **Accrual run**: face × coupon rate × days / year days from the last accrual to the period end
  (or maturity). A coupon receipt, maturity or sale first accrues up to its date, then relieves the
  whole accrued interest; any difference goes to interest income (coupons) or the realized result.
- **Amortization run** (`InterestCalculator`): *effective interest* — the effective annual rate y is
  solved by bisection at approval so that, stepping month end by month end, carrying + carrying ×
  ((1 + y)^(days/365) − 1) − coupon reaches face value at maturity; each period amortizes carrying ×
  ((1 + y)^(days/365) − 1) − coupon accrued. *Straight line* — (face − carrying) × days / days to
  maturity. The period ending at maturity amortizes the remaining difference. Bought at par or
  equities: none.
- **Fair value** (FVOCI / FVPL only): amortization is brought up to the valuation date, then the
  difference between fair value and carrying amount is posted to the portfolio's fair value account.
- **Maturity / sale**: realized gain = proceeds + final tax + FVOCI reserve recycled − carrying
  amount − accrued interest.

## Reports (category Financial Statements, permission REPORT_FINANCIAL)

| Code | Title |
|---|---|
| FIN-FA-REG | Fixed Asset Register (as of a date, by category) |
| FIN-FA-DEPR | Depreciation Schedule (runs in a date range, by period) |
| FIN-FA-MOVE | Asset Movement Schedule (additions, disposals, transfers) |
| FIN-FA-NBV | Net Book Value by Category (by branch) |
| FIN-INV-PORT | Investment Portfolio (by instrument type or classification) |
| FIN-INV-ACCR | Interest Accrual Register (accruals and amortization) |
| FIN-INV-MAT | Investment Maturity Profile (remaining-term buckets) |
| FIN-INV-RGL | Realized Gains and Losses on Investments |
| FIN-INV-SECDEP | Security Deposit Schedule (Insurance Commission) |

## Permissions

View: MASTER_VIEW. Categories, portfolios, assets, holdings: MASTER_MAINTAIN (maker) and
MASTER_AUTHORIZE (checker: authorize, capitalize, approve). Month-end runs: PERIOD_END_RUN.
Disposals and transfers: ASSET_MANAGE; coupons, maturities, sales and fair value: INVESTMENT_MANAGE
(both granted to FIN_MANAGER, ACCOUNTANT and AUTHORIZER in V671).

## Demo data (demo profile)

`FixedAssetDemoData` and `InvestmentDemoData` (`@Order(70)`, idempotent) build 25 assets (16 taken on
at 2026-01-01, 9 acquired in 2026), depreciation runs January–September 2026, one transfer and one
vehicle sale; and 12 holdings (5 taken on, 7 bought in 2026) with coupons, accrual and amortization
runs January–September 2026, two maturities, one sale and 30 June fair value updates.
