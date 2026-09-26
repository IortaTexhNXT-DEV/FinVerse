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

## Deployment view (AWS, BDOI IER)

BIBS runs on Amazon EKS in ap-southeast-1 with managed AWS services; the application stays one modular monolith
([`ARCHITECTURE_OPTION_DECISION.md`](ARCHITECTURE_OPTION_DECISION.md): BIBS modular monolith with the IER enterprise
elements, awaiting BDOI confirmation, IQ25 / DCR-222).

| Layer | Component |
|---|---|
| Edge | Route 53, AWS WAF, Application Load Balancer (or Apigee X if BDO mandates it) in front of the ingress |
| Workloads | `brokerverse-frontend` (nginx, React SPA) and `brokerverse-backend` (Spring Boot, Java 21), 2 to 8 pods per environment with HPA and PodDisruptionBudget (`deploy/k8s/brokerverse.yaml`; sizing in PROGRAMME_ALIGNMENT section 6.4) |
| Data | Amazon RDS PostgreSQL 16 Multi-AZ (one database, one transaction per business record), ElastiCache Redis 7 (cluster mode disabled), Amazon MSK (Kafka 3.6, 9 `bibs.*` topics with transactional outbox) |
| Files | Amazon S3 for every document and attachment (four buckets per environment, SSE-KMS with BDOI keys, Object Lock governance mode, GuardDuty malware scan); PostgreSQL keeps the metadata ([`DOCUMENT_STORAGE_DECISION.md`](DOCUMENT_STORAGE_DECISION.md)) |
| Identity | EIAM (Microsoft Entra ID, OpenID Connect) for sign-in; UIDM-ISC (IGA) for provisioning (USER_ACCESS_DESIGN section 10.1) |
| Environments | DEV, SIT, UAT, Pre-Prod, PROD, DR (warm standby in the DR region, RDS cross-region replica, S3 replication); RPO 15 minutes, RTO 4 hours |

The IER-aligned diagrams for BDOI IT are `docs/deliverables/out/Programme/Alignment/IER/BIBS_IER_Application_Architecture.png`
and `BIBS_IER_Infrastructure_Deployment.png` (sources `docs/deliverables/src/alignment/figures/`); the comparison with
the IER workbook is [`PROGRAMME_ALIGNMENT.md`](PROGRAMME_ALIGNMENT.md) section 6.

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
