# iNXT BrokerVerse – Solution Architecture

## Context

```
 Users (BDOI personas: Marketing, TSU, Processing, Operations, Collections, Accounting, Compliance, admin)
        │ HTTPS
        ▼
 ┌──────────────────┐  /api  ┌─────────────────────────────┐   JDBC   ┌──────────────┐
 │ Frontend (nginx) │ ─────► │ Backend (Spring Boot, JWT)  │ ───────► │ PostgreSQL 16 │
 │ React SPA        │        │ modular monolith            │          │ Flyway schema │
 └──────────────────┘        └─────────────────────────────┘          └──────────────┘
```

## Backend modules

This table lists the platform modules of the original finance suite. The BDOI broking modules (BRD-1 to BRD-13) are
described in [`BROKING_ARCHITECTURE.md`](BROKING_ARCHITECTURE.md) and the design of each BRD. Modules marked
*insurer-side* are not used by any BDOI process and are hidden from BDOI roles; see
[`CODEBASE_RELEVANCE_AUDIT.md`](../development/CODEBASE_RELEVANCE_AUDIT.md).

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
| `underwriting` | *Insurer-side.* Products, quotations, policies, endorsements, open covers, UW reports |
| `claims` | *Insurer-side* (BDOI claims are in `brokerclaims`). Claims, estimates, settlements, recoveries, LPOs, claims reports |
| `reinsurance` | *Insurer-side* (the BDOI ReInsurance BRD is phase 2). Treaties, cessions, facultative, RI claims recoveries, statements of account |
| `reserves` | *Insurer-side.* UPR, DAC, IBNR, OSLR and surplus processing |
| `payables` | Bank accounts, supplier invoices, payment vouchers, PDC issued, petty cash |
| `receivables` | Receipts, cheques & deposits, PDC received, bank reconciliation |
| `finreport` | Finance / MIS report book |
| `budget`, `consolidation`, `closing` | Budgets, inter-company & consolidation (*not used by BDOI*), FX revaluation, period close, year-end |

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

- `docs/requirements/BDOI_*_BRD_SPEC.md` – the BDOI requirements per BRD; `BDOI_ACCT_BRD_SPEC.md` (BRD-5) is the
  baseline of the GL, sub-ledger, tax and finance reports for BDOI.
- `docs/requirements/GL_FUNCTIONAL_SPEC.md` – GL functional baseline of the original suite (Premia GL documents).
- `docs/requirements/FINANCE_REPORTS_SPEC.md` – finance reports (Annexure 2(b)).
