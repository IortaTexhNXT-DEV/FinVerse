# Planning & Closing – budgets, FX revaluation, year-end

Modules `budget` and `closing` (backend) and `features/budget`, `features/closing` (frontend, menu
section "Planning & Closing"). Migrations `V400` (FX revaluation, year-end) and `V600` (budgets);
seed data `V960`/`V961` plus the start-up runner `closing.seed.PlanningSeedData` (`seed` profile,
`@Order(60)`, idempotent). Inter-company and group consolidation of the original suite were removed
with the insurer suite (their tables dropped by V2501, see
[`CODEBASE_RELEVANCE_AUDIT.md`](CODEBASE_RELEVANCE_AUDIT.md) A6).

## Budgets (`/api/v1/budgets`, permission `BUDGET_MANAGE`)

- A **version** belongs to a company and fiscal year: `ORIGINAL` (one per year) or `REVISED`
  (copies the latest approved version). Lifecycle `DRAFT → SUBMITTED → APPROVED`, `REJECTED`
  back to the maker; approving a revision marks the previous approved version `SUPERSEDED`.
  The submitter can never approve (maker-checker). Accountants prepare, finance managers approve.
- **Lines**: income or expense account × optional cost centre × 12 monthly amounts, natural sign.
  Tools: spread evenly or by seasonality (grid), CSV import (`docs/samples/budget_import_sample.csv`,
  `budget_import_annual_sample.csv`), copy prior-year actuals ± %.
- **Monitoring** always uses the latest approved version: `GET /variance` (month + YTD budget,
  actual, variance = actual − budget, variance %, utilization = YTD actual / annual budget) and
  `GET /alerts?threshold=90` ("Budget threshold exceeded": expense accounts at or above the
  threshold). Actuals come from the ledger and exclude year-end `CLOSING` journals.
- Reports: `GL-BVA` Budget vs Actual, `GL-BUTIL` Budget Utilization.

## FX revaluation (`/api/v1/closing/fx-revaluations`, permission `PERIOD_END_RUN`)

- Scope: postable balance sheet accounts flagged `revaluationRequired`, every branch and currency
  other than the base currency. Revalued base = FC balance × CLOSING rate on the period end.
- Posting ("restatement" in the FC bucket): per balance the booked base is taken out at the
  booked average rate and the FC amount put back at the closing rate, so the FC balance is
  unchanged; the difference goes to the unrealized gain/loss account (default 4602) on the same
  branch. One `REVALUATION` journal per period; **idempotent** (a revalued period returns its run).
- Optional **auto-reversal** on the first day of the next period (posted immediately when that
  period accepts postings, otherwise before the next revaluation run).
- A missing CLOSING rate blocks posting. Foreign currency open items are revalued for information
  only. Report `GL-FXREV` FX Revaluation Register.

## Period-end and year-end (`/api/v1/closing`)

- **Checklists** (automatic pass/fail): period status, pending journals (draft / pending / rejected),
  unreconciled items, FX revaluation status, trial balance. Unreconciled items come from beans
  implementing the port `closing.service.ReconciliationStatusProvider`; today the bank
  reconciliation of receivables (`BankReconciliationStatusProvider`: book entries and statement
  lines of every bank account not reconciled up to the period end, the BRS definition, see
  [RECEIVABLES_AND_BANKING.md](../modules/RECEIVABLES_AND_BANKING.md)). The control
  `RECONCILIATIONS` is a **warning** (`CheckItem.warning`, non-blocking): reconciling items such as
  deposits in transit and unpresented cheques are normal at a period end and are carried in the
  BRS, so they are shown for review ("n unreconciled item(s) up to <date> (Bank reconciliation n)")
  but do not block the close or the year-end close; the screen shows them with a *Warning* badge
  and "ready to close, review the warnings".
  The port stays in closing: receivables depends on closing and closing depends on no module that
  depends on receivables, so no neutral kernel package is needed.
  Other modules append their own period-end controls through the port
  `closing.service.PeriodEndCheckProvider`.
- **Year-end close** (`YEAR_END_CLOSE`): requires every period CLOSED or CLOSING with the final
  period in CLOSING (it receives the closing journal), no pending journals, a balanced TB, the
  final period revalued (or nothing to revalue) and a valid company retained earnings account;
  unreconciled items are listed as a warning only. It posts one `CLOSING` journal per branch dated the last day of the year that
  zeroes every income and expense balance (per account, cost centre and line of business, base
  currency) against retained earnings, closes the remaining periods, marks the fiscal year CLOSED
  through the hook `PeriodService.closeFiscalYear` and creates the next fiscal year (first
  period opened) when missing.
- **Carry forward is implicit**: the ledger is cumulative, so balance sheet balances at year end
  are the opening balances of the next year; no opening balance journal is generated.
- **Not implemented**: reversal of a year-end close. A closed fiscal year cannot be reopened
  (`PeriodService.reopen` refuses periods of a closed year); a reopen + reverse flow would need a
  period-module change and is left open.
- The closing journal is dated the last day of the year, so an income statement run to that date
  that includes `CLOSING` journals shows zero; budget actuals exclude them.

## Seed data

FVI FY2026 approved budget; FVI FX revaluations July and August with auto-reversal. The second seed
company FVS "BDO Insurance and Reinsurance Brokers (Singapore) Pte. Ltd." (USD, V960, named in V989)
keeps its copy of the seed chart and FY2026; it has no journals of its own since inter-company and
consolidation were removed.
