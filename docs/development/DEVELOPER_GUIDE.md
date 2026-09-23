# iNXT FinVerse – Developer Guide

This guide is binding for everyone who changes FinVerse. Following it keeps the code base uniform,
so the production support team can understand, trace and fix any module the same way.

## 1. Architecture in one page

- **Modular monolith.** One Spring Boot application (`backend/`) and one React SPA (`frontend/`),
  deployed as two containers against one PostgreSQL database.
- **Modules = top-level packages** under `com.iortatechnxt.finverse`. Each module has the same inner
  layout:

  | Package   | Contains                                                                  |
  |-----------|---------------------------------------------------------------------------|
  | `domain`  | JPA entities, enums, value records, Spring Data repositories              |
  | `service` | Transactional business logic, ports (interfaces) other modules implement  |
  | `api`     | `@RestController` classes; `api/dto` holds request/response records       |

- **Rules enforced by `ArchitectureTest` (build fails otherwise):**
  - no dependency cycles between modules;
  - `domain` never depends on `service` or `api`;
  - services never depend on controllers;
  - controllers live in `..api..`.
  If module A must call into a module B that already depends on A, declare an interface (a *port*) in
  A and implement it in B (example: `coa.service.AccountUsageChecker` implemented by `ledger`, and
  `period.service.PeriodCloseGuard` implemented by `journal`).

### Core flow

```
Operational module (policy, claim, receipt, invoice…)
   │  AccountingEventPublisher.publish(BusinessEvent)          ← never pick GL accounts yourself
   ▼
Accounting engine (accounting)  – selects the AccountingRule, builds balanced lines
   ▼
SystemJournalService (journal)  – validates, numbers, auto-authorizes, posts (same transaction)
   ▼
PostingService → gl_ledger_entry (immutable) + gl_daily_balance (upsert)
   ▼
Reports / dashboard read LedgerQueryService / FinancialStatementService
```

Party balances are kept in the **open-item sub-ledger** (`subledger`): every document that creates a
receivable or payable records an `OpenItem` in the same transaction as its journal.

## 2. Shared building blocks (reuse, never duplicate)

| Need | Use |
|---|---|
| Base columns (id, version, created/updated by/at) | extend `common.domain.BaseEntity` |
| Maker-checker master data | extend `common.domain.AuthorizableEntity` (`markModified()`, `authorize()`) |
| Error for a broken business rule | `throw new BusinessRuleException("UPPER_SNAKE_CODE", "Readable message")` → HTTP 422 |
| Not found / duplicate | `ResourceNotFoundException` (404), `DuplicateResourceException` (409) |
| Current user | `common.security.CurrentUser` |
| Time | inject `java.time.Clock` (never `LocalDate.now()` without a clock) |
| Money | `BigDecimal` only, scale 2; helpers in `common.util.Money` |
| Document numbers | `common.sequence.DocumentNumberService.next("PREFIX-BRANCH-YYYY")` |
| Audit trail | `audit.service.AuditTrailService.record(entity, key, AuditAction, summary)` on every change |
| GL posting from business events | `accounting.service.AccountingEventPublisher.publish(BusinessEvent)` |
| Direct system journal (period end) | `journal.service.SystemJournalService.post(SystemJournalRequest)` |
| Business partners | `party.service.PartyService` (`requireActive(companyId, code, types)`) |
| Receivables / payables | `subledger.service.OpenItemService` (`record`, `match`, `allocateFifo`) and `AgeingService` |
| Period check | `period.service.PeriodService.requirePostingPeriod(...)` |
| Paged API result | `common.api.PageResponse.of(page, Dto::from)` |
| Reason payloads | `common.api.ReasonRequest` |
| Reports | implement `report.core.ReportDefinition`; build with `TabularReportBuilder` |

## 3. Coding conventions (Java)

- Java 21, Spring Boot 3.5. Constructor injection only; no field injection; no Lombok.
- Entities: fields private, protected no-arg constructor for JPA, business methods that enforce
  invariants (see `JournalBatch`), getters; setters only for freely editable attributes.
- DTOs are Java **records** in `api/dto` with Jakarta validation annotations and a static
  `from(entity)` mapper. Controllers never return entities.
- Services: `@Service @Transactional` at class level, `@Transactional(readOnly = true)` for queries.
  Return entities to controllers only when every association the DTO touches is loaded (use
  `@EntityGraph(type = LOAD)` on the repository method).
- Every endpoint has `@PreAuthorize("hasAuthority('PERMISSION')")`. Permissions are the enum
  `security.domain.Permission`; grant them to roles in a Flyway migration.
- Public types need Javadoc (checkstyle). Methods: Javadoc on public API methods, explaining *why*
  where it is not obvious.
- Limits (build fails above them): method 60 lines, cyclomatic complexity 10, cognitive complexity
  15, 7 parameters (constructors exempt), file 600 lines, no magic numbers, no duplicated literals
  (≥ 4), no copy-pasted blocks (CPD 120 tokens).
- Formatting is automatic: run `mvn spotless:apply` before committing.

## 4. Database

- Flyway only; never edit an applied migration. Naming `V<version>__<snake_description>.sql`.
- Version ranges (avoid collisions between teams):

  | Range | Owner |
  |---|---|
  | V1–V99 | platform (foundation, engine, party, sub-ledger) |
  | V100–V199 | underwriting |
  | V200–V299 | claims |
  | V300–V399 | reinsurance |
  | V400–V499 | period-end, actuarial reserves, FX revaluation, year-end |
  | V500–V549 | payables, petty cash, payments, PDC issued |
  | V550–V599 | receivables, collections, PDC received, bank reconciliation |
  | V600–V649 | budgets, consolidation, inter-company |
  | V650–V699 | finance / MIS reports support objects |
  | V900–V999 | demo data (`db/demo`, loaded only with the `demo` profile) — same sub-ranges: underwriting V910s, claims V920s, reinsurance V930s, period-end V940s, payables V950s, receivables V955s, budget V960s |

- Conventions: snake_case, `bigint generated by default as identity` keys, `numeric(19,2)` money,
  `numeric(19,8)` rates, `varchar(3)` currencies, audit columns as in V1, foreign keys and indexes
  for every lookup path, check constraints for invariants.
- Hibernate runs with `ddl-auto: validate`: entity and table must match exactly.

## 5. Accounting rules for developers

- Operational modules publish a `BusinessEvent` with an **event type** (table `acc_event_type`),
  amount **components** and optional **account roles** (`@BANK`, `@EXPENSE`…). Add new event types
  in your module's migration, with demo rules in your demo migration.
- Use a unique `sourceReference` per business transaction (e.g. `POLICY:123:ENDT:2`): the engine is
  idempotent on it.
- Negative component amounts post to the opposite side (refunds, releases).
- Sub-ledger party goes in `partyCode`; line of business in `businessLine` (mandatory for premium,
  claims and reinsurance accounts in the standard chart).

## 6. Reports

- One class per report implementing `ReportDefinition`, annotated `@Component`, in the owning
  module's `report` sub-package (e.g. `underwriting.report.PremiumRegisterReport`).
- Report codes: GI reports keep the codes of the Reports Book (e.g. `PGIBR015`); finance reports use
  `FIN-…` codes from `docs/requirements/FINANCE_REPORTS_SPEC.md`; GL reports `GL-…`.
- Declare parameters with `ParameterSpec` (dates default via `TODAY`, `MONTH_START`, `YEAR_START`).
  The UI builds the parameter form automatically; PDF/Excel/CSV export is automatic.
- Use `TabularReportBuilder` for grouped layouts (`groupBy` = Branch > Class > Product…), totals
  are computed for summed columns.
- Heavy reports: query with SQL/JPQL aggregates, not by loading every entity.

## 7. Frontend conventions

- `src/api/<module>.ts` – typed DTO interfaces + an `xxxApi` object of functions using `api` from
  `api/client.ts`.
- `src/features/<module>/` – pages (`XxxPage.tsx`, default export), feature components and a
  `module.ts` exporting a `FeatureModule` (menu section + screens with permission). Register it in
  `src/navigation/modules.ts`. Routes and menu are generated from it.
- Data fetching with TanStack Query (`useQuery` / `useMutation`), invalidate the list query after a
  mutation, show `ErrorAlert` for errors and `useToast()` for success.
- UI kit only (`components/ui`): `PageHeader`, `Card`, `DataTable`, `Field`, `Modal`, `Button`
  (`accent` = the main call to action), `StatusBadge`, `Kpi`, `Tabs`, `Amount`. No ad-hoc colours:
  use CSS tokens from `styles/tokens.css` (BDO blue/navy + gold).
- Accessibility: every input has a label (`Field`), buttons have text or `aria-label`.
- Lint (SonarJS + strict TS) must be clean: `npm run lint && npm run typecheck`.

## 8. Tests (definition of done)

- Service logic: integration test annotated `@IntegrationTest` (embedded PostgreSQL, demo data) –
  use `AsUser` to act as demo users (`accountant`, `checker`, `fmanager`, `uw`, `claims`,
  `reinsurer`, `auditor`; password `Finverse@2026`).
- Every new read endpoint added to `ApiSmokeIT`.
- Every report exercised in a test that runs it and exports PDF/XLSX/CSV.
- Pure logic (calculations): plain JUnit tests.
- Frontend: Vitest for calculations and components.
- Coverage gate: 80 % lines / 65 % branches (backend).

## 9. Local commands

```bash
# backend (needs Java 21, Maven)
cd backend
mvn spotless:apply            # format
mvn verify                    # format check, compile (-Werror), tests, coverage, checkstyle, PMD, CPD, SpotBugs
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run   # needs PostgreSQL on localhost:5432 (docker compose up db)

# frontend (Node 22)
cd frontend
npm ci
npm run verify                # prettier, SonarJS lint, typecheck, tests with coverage
npm run dev                   # http://localhost:5173 (proxies /api to :8080)
```
