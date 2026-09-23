# Planning & Closing – budgets, inter-company, consolidation, FX revaluation, year-end

Modules `budget`, `consolidation` and `closing` (backend) and `features/budget`,
`features/consolidation`, `features/closing` (frontend, menu section "Planning & Closing").
Migrations `V400` (FX revaluation, year-end) and `V600` (budgets, inter-company, consolidation);
demo data `V960`/`V961` plus the start-up runner `closing.demo.PlanningDemoData` (`demo` profile,
`@Order(60)`, idempotent).

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

## Inter-company (`/api/v1/intercompany`, permission `CONSOLIDATION_RUN`)

- A **relationship** per company pair defines each company's due-from and due-to accounts
  (dedicated to that counterparty). Only active relationships transact.
- A **transaction** (`CHARGE` or `SETTLEMENT`) posts two mirror system journals (type
  `CONSOLIDATION`, source `INTERCOMPANY`) with the same `IC-YYYY-nnnnnn` reference and value date
  in one database transaction – both post or neither does. Base amounts are converted by the
  journal engine at the value date's SPOT rate (the rate table is quoted against PHP).
- **Reconciliation** compares due-from with the counterparty's due-to per currency in transaction
  currency (report `GL-ICREC`).

## Consolidation (`/api/v1/consolidation`, permission `CONSOLIDATION_RUN`)

- **Group**: parent company, consolidation currency, CTA / NCI / goodwill accounts, subsidiaries
  with ownership %, the parent's investment account and the subsidiary's capital accounts. The
  group chart of accounts is the parent's chart: members are aggregated by account code.
- **Run** as of a date: each member's trial balance (cumulative, memorandum accounts excluded) is
  translated – assets, liabilities and equity at the CLOSING rate, income and expenses at the
  AVERAGE rate (latest AVERAGE rate of the fiscal year, else the mean of the year's SPOT rates) –
  and the resulting CTA is booked to the translation reserve. Eliminations:
  `IC_BALANCE` (due-from vs due-to of every active relationship inside the group, difference to
  CTA) and `INVESTMENT_EQUITY` (investment vs subsidiary capital, minority share to NCI,
  remainder to goodwill). Every rule is balanced, so the consolidated TB balances (checked).
  Company ledgers are never modified. A re-run cancels the previous `DRAFT`; a `FINAL` run blocks
  duplicates for the same date.
- Reports: `GL-CON-TB`, `GL-CON-BS`, `GL-CON-PL`, `GL-CON-ELIM` (parameter `groupCode`, optional
  `asOfDate` → latest run on or before it).
- Simplifications: equity is translated at the closing rate (no historical rates); IC profit in
  stock, dividends and interest are not eliminated.

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
  implementing the port `closing.service.ReconciliationStatusProvider` (zero while none exists).
- **Year-end close** (`YEAR_END_CLOSE`): requires every period CLOSED or CLOSING with the final
  period in CLOSING (it receives the closing journal), no pending journals, a balanced TB, the
  final period revalued (or nothing to revalue), no unreconciled items and a valid company retained
  earnings account. It posts one `CLOSING` journal per branch dated the last day of the year that
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

## Demo data

FVS "FinVerse Demo Insurance (Singapore)" (USD) with a copy of the demo chart, FY2026 and monthly
journals; FVI owns 80 % (group `FVGRP`, investment 1506 vs capital 3100); management fees FVI → FVS
March–August with one settlement (IC accounts 1607 / 2510); FVI FY2026 approved budget; FVI FX
revaluations July and August with auto-reversal; a consolidation run as of 31 August 2026.
