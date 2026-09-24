# iNXT BrokerVerse – Solution Architecture

## Context

```
 Users (finance, underwriting, claims, reinsurance, auditors)
        │ HTTPS
        ▼
 ┌──────────────────┐  /api  ┌─────────────────────────────┐   JDBC   ┌──────────────┐
 │ Frontend (nginx) │ ─────► │ Backend (Spring Boot, JWT)  │ ───────► │ PostgreSQL 16 │
 │ React SPA        │        │ modular monolith            │          │ Flyway schema │
 └──────────────────┘        └─────────────────────────────┘          └──────────────┘
```

## Backend modules

| Module | Responsibility |
|---|---|
| `common` | Base entities, maker-checker, errors, money, sequences, current user |
| `security` | Users, roles, permissions, JWT, lockout, bootstrap |
| `audit` | Insert-only audit trail (UPDATE, DELETE and TRUNCATE rejected by database triggers, V26) |
| `organization` | Companies, branches (Office Master), holiday calendars |
| `currency` | Currencies, rate types, exchange rates, conversion |
| `coa` | Multi-tier chart of accounts, GL categories, posting controls |
| `dimension` | Cost centres, lines of business, departments, profit centres |
| `period` | Fiscal years, monthly periods, period lifecycle, close guards |
| `journal` | Journal batches, maker-checker authorization, posting engine, reversal, system journals |
| `ledger` | Immutable ledger entries, daily balances, balance & statement queries |
| `party` | Business partners (clients, intermediaries, reinsurers, coinsurers, suppliers) |
| `subledger` | Open items, matching, ageing (receivables, payables, RI balances) |
| `accounting` | Event-driven accounting engine: event catalogue, rules, simulator, event register |
| `report` | Report framework, renderers (PDF/XLSX/CSV), GL & financial statement reports |
| `dashboard` | Executive KPIs |
| `underwriting` | Products, quotations, policies, endorsements, open covers, UW reports |
| `claims` | Claims, estimates, settlements, recoveries, LPOs, claims reports |
| `reinsurance` | Treaties, cessions, facultative, RI claims recoveries, statements of account |
| `reserves` | UPR, DAC, IBNR, OSLR and surplus processing |
| `payables` | Bank accounts, supplier invoices, payment vouchers, PDC issued, petty cash |
| `receivables` | Receipts, cheques & deposits, PDC received, bank reconciliation |
| `finreport` | Finance / MIS report book |
| `budget`, `consolidation`, `closing` | Budgets, inter-company & consolidation, FX revaluation, year-end |

Dependency direction is enforced by ArchUnit: operational modules → accounting → journal → ledger;
no cycles. Cross-module call-backs use ports (interfaces owned by the caller).

## Key design decisions

1. **Event-driven accounting.** Operational modules publish business events; configurable,
   authorized rules produce the journal. Accountants change postings without code changes.
2. **On-line transaction posting.** The business record, its journal, ledger entries, balances,
   sub-ledger open items and audit rows commit in one database transaction.
3. **Immutable ledger + daily balances.** Detail rows are never updated (DB trigger); a daily
   balance table (atomic upsert) makes trial balances and statements fast at scale.
4. **Maker-checker everywhere.** Masters extend `AuthorizableEntity`; journals enforce
   segregation of duties and authorization limits.
5. **One report engine.** Every report returns a typed `ReportResult`; screen, PDF, Excel and CSV
   renderings are shared, so all reports look and behave alike.
6. **Money correctness.** `BigDecimal`, banker's rounding, base and transaction currency on every
   line, rounding differences corrected deterministically.

## Requirements traceability

- `docs/requirements/GL_FUNCTIONAL_SPEC.md` – GL functional baseline (Premia GL documents).
- `docs/requirements/REPORTS_BOOK_SPEC.md` – GI reports (Annexure 2(c)).
- `docs/requirements/FINANCE_REPORTS_SPEC.md` – finance reports (Annexure 2(b)).
