# iNXT BrokerVerse – Developer Guide

This guide is binding for everyone who changes BrokerVerse. Following it keeps the code base uniform,
so the production support team can understand, trace and fix any module the same way.

## 1. Architecture in one page

- **Modular monolith.** One Spring Boot application (`backend/`) and one React SPA (`frontend/`),
  deployed as two containers against one PostgreSQL database.
- **Modules = top-level packages** under `com.iortatechnxt.brokerverse`. Each module has the same inner
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
  - controllers live in `..api..`;
  - no `@Scheduled` methods: background work is a `ManagedJob` (section 10.3).
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
  | V700–V749 | tax & statutory reporting (BIR / LGU / BFP returns, 2307, IC schedules) |
  | V750–V759, V790–V799 | broking foundations (lov, workflow, bulk, messaging, docgen) and broking administration |
  | V760–V789 | Operations (BRD-2): foundation, invoice ledger and platform extensions V760–V762, then cashiering, remittance, product reconciliation, adjustment and commission receivables in their own sub-ranges |
  | V800–V889 | broking business modules (crm V800s, catalog V810s, account V820s, quotation V830s, non-package V840s, placement V850s, issuance V860s, booking V870s, NB reports V880s) |
  | V890–V899 | Accounting, Disbursement and ACSL (BRD-5): disbursement V891–V893, payrequest V894–V895, acsl V896–V897, frbs V898–V899, foundation V890 |
  | V1000–V1899 | modules of later BRDs, 10 versions each: Collections (BRD-4) V1000–V1009, then the next BRD V1010–V1019 and so on |
  | V900–V999 | demo data (`db/demo`, loaded only with the `demo` profile) — same sub-ranges: underwriting V910s, claims V920s, reinsurance V930s, period-end V940s, payables V950s, receivables V955s, budget V960s, tax V975–V979, broking V980–V989, Operations V990–V995, Product Maintenance V996–V997, Accounting / Disbursement V998–V999 (full) |
  | V1900–V1999 | demo data of the V1000+ modules, 10 versions each in the same order: Collections V1900–V1909, then V1910–V1919 … (runs after all V9xx demo, so it can build on the Operations and booking demo) |

  Because each module owns a range, a module can add a migration whose version is lower than one
  another module has already applied (for example a new platform `V27` after underwriting's `V101`).
  Flyway therefore runs with `spring.flyway.out-of-order: true`. Never renumber or edit an applied
  migration; add a new one in your range. Migrations of different modules must not depend on each
  other's order unless the lower-numbered one is certain to have been applied first.

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
  `reinsurer`, `auditor`; password `Brokerverse@2026`).
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

## 10. Platform services for business modules

These cross-cutting services exist once; business modules plug into them. All plug points are
**ports owned by the platform module that your module implements** (your module depends on the
platform module, never the reverse), so ArchUnit stays cycle-free.

### 10.1 Approval inbox ("My Approvals") – `approval`

Every document that waits for a checker must appear in the universal inbox. Implement
`approval.service.PendingApprovalSource` as a Spring bean in **your** module's `service` package:

```java
@Component
public class PolicyApprovalSource implements PendingApprovalSource {
  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can("POLICY_AUTHORIZE")) {
      return List.of();
    }
    return policies.findPendingAuthorization().stream()
        .filter(p -> viewer.mayApproveItemOf(p.getSubmittedBy())) // maker never sees own items
        .map(p -> new PendingApproval("UNDERWRITING", "Policy", p.getPolicyNo(), p.getInsuredName(),
            p.getGrossPremium(), p.getCurrency(), p.getSubmittedBy(), p.getSubmittedAt(),
            p.getCompanyId(), "/underwriting/policies/" + p.getId()))
        .toList();
  }
}
```

- `ApprovalViewer.system()` (used by the `PENDING_APPROVAL_AGEING` alert) must return **all**
  pending items: `can()` and `mayApproveItemOf()` are always true for it.
- `link` is the frontend route that opens the item (the inbox navigates there).
- Maker-checker master data (`AuthorizableEntity`) needs no custom query: use
  `approval.service.MasterRecordApprovals.pending(viewer, Entity.class, e -> new RecordFacts(...))`
  (see `organization.service.OrganizationApprovalSource`). Records a module authorizes under its own
  permission pass a `Scope(module, permission)`, e.g. products under `POLICY_AUTHORIZE`
  (`underwriting.service.UnderwritingApprovalSource`).
- Apply the same checks as the approval itself: the permission of the approve endpoint, every user
  the approval refuses (creator *and* submitter where both are checked), and the authorization limit
  where the approval enforces one (`JournalApprovalSource`, `PayablesApprovalSource`,
  `ClaimApprovalSource`, `UnderwritingApprovalSource`).
- Sources today: GL journals, accounting rules, chart of accounts, parties, organization,
  underwriting (policies, endorsements, quotations, products, open covers), payables (invoices,
  vouchers, petty cash, bank accounts, funds), receivables (receipts), budget (submitted versions),
  fixed assets (capitalization, categories), investments (holdings, portfolios), actuarial reserves
  (valuation runs, reserve parameters). Consolidation runs have no maker-checker step, so they have
  no source.
- The `PENDING_APPROVAL_AGEING` check reads `ApprovalInboxService.pendingAll()` (the system view of
  every source) and raises one alert per item, de-duplicated on module, type and reference.
- API: `GET /api/v1/approvals/inbox?companyId=`, `GET /api/v1/approvals/counts` (header badge).

### 10.2 Exception codes and alerts – `alert`

- **Seed your exception codes** in your module's migration:
  `insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
  threshold_days, created_at, created_by) values (...)`. Administrators tune severity, thresholds
  and activation on *Administration → Exception Codes*.
- **Event-time conditions**: call `alert.service.AlertService.raise(code, new AlertFacts(companyId,
  branchId, entityType, entityId, message, amount, dedupKey))` in your transaction. Nothing is
  raised when the code is inactive or an alert with the same `dedupKey` is still open or
  acknowledged – choose the key so it identifies the condition (e.g. `"CLAIM_OVER_RESERVE:" +
  claimId`). Read thresholds with `AlertService.activeCode(code)`.
- **Scheduled conditions**: implement `alert.service.AlertCheck` (returns `AlertSignal`s); the
  daily `ALERT_DAILY_CHECKS` job evaluates every check and raises the signals.
- Journal posting rules live in `alert.service.JournalPostingAlertRules`, which implements the
  port `journal.service.JournalPostingListener` (called by `PostingService` inside the posting
  transaction).
- The exception report `CTL-EXCEPTIONS` lists all alerts; filter by `code` for a module view.

### 10.3 Background jobs – `system`

Implement `system.service.ManagedJob` (name, description, cron, `execute(businessDate)`); do **not**
use `@Scheduled` (the ArchUnit rule `BACKGROUND_WORK_IS_A_MANAGED_JOB` fails the build).
`JobScheduler` schedules it (UTC cron, `"-"` = manual only), `JobRunService`
records every run in `sys_job_run`, a failure raises `JOB_FAILURE`, and administrators see it on
*Administration → Scheduled Jobs* with "Run now". For batch runs started from your own screen, wrap
the work in `JobRunService.execute(jobName, JobTrigger.MANUAL, () -> new JobOutcome(n, message))`.
Make the cron configurable (`brokerverse.jobs.<name>-cron`), add it to `application.yml` with an
environment variable and document it in `docs/operations/CONFIGURATION.md`. Jobs today:
`RECURRING_JOURNALS`, `ALERT_DAILY_CHECKS`, `PDC_ISSUED_DUE`, `QUOTATION_EXPIRY`, `HOLD_COVER_EXPIRY`,
`BOOKING_BATCH` (daily), `PAYMENT_CONFIRMATION_SWEEP` (hourly), `MAIL_DISPATCH` (every two minutes),
`KYC_REVIEW_DUE`, `RETENTION_REVIEW` (monthly) and `RESERVE_VALUATION`, `RI_ALLOCATION`,
`QUOTATION_REQUEST_INTAKE`, `OPS_INVOICE_FEED_REPLAY` (manual unless scheduled). The crons of the Operations jobs built on top of the ledger are already configured (`brokerverse.jobs.prebooked-rematch-cron` … `dp-feedback-sla-cron`, see `docs/modules/OPERATIONS.md`).

### 10.4 Business parameters – `system`

Read shared parameters with `SystemParameterService.intValue/text/items(KEY, fallback)`.
`AGEING_BUCKETS` is the default of every debtors / creditors ageing report
(`subledger.service.AgeingService.defaultSlots()`, slots in `subledger.service.AgeingSlots`);
`REPORT_FOOTER_TEXT` is printed at the foot of every PDF page. Add a
parameter with an insert into `sys_parameter` in your migration (type `STRING`, `INTEGER`,
`DECIMAL`, `BOOLEAN`, `INTEGER_LIST` or `CODE_LIST`, optional min/max). Never store secrets there.

### 10.5 Attachments – `attachment`

Any record can carry documents: frontend `<Attachments entityType="Policy" entityId={policy.id} />`
(`components/attachments/Attachments`); API `/api/v1/attachments?entityType=&entityId=`. Files are
stored in PostgreSQL with SHA-256 checksum, type/signature and size checks
(`brokerverse.attachments.max-size`, default 10 MB) and audit entries. Malware scanning: add a bean
implementing `attachment.service.VirusScanner`. Permissions `ATTACHMENT_VIEW` / `ATTACHMENT_MANAGE`.

### 10.6 Executive dashboard – `dashboard`

`GET /api/v1/dashboard` is the ledger summary; `/dashboard/{premium,claims,collections,payables,
cash,budget,workload}` serve one widget each (optional `branchId` and `asOf`), so a widget without
data or with an error never blanks the others. Ledger figures come from constant SQL over platform
tables (`DashboardLedgerQueries`); the accounts are mapped in `brokerverse.dashboard.*`
(`DashboardProperties`: statement lines for premium and cash, account prefixes for claims paid and
the outstanding claims reserve). Collections come from `receivables.service.CollectionQueries` and
the budget from `BudgetMonitoringService`: the dashboard depends on those modules' query services,
never the reverse.

### 10.7 Help content

Every screen has an entry in the in-app Help Center (summary, workflow, controls). A module keeps
its section in `frontend/src/features/<module>/help.ts` (e.g. `UNDERWRITING_HELP`) and registers it
in `HELP_SECTIONS` (`frontend/src/features/help/helpContent.ts`) in sidebar order. Every non-hidden
screen route of a `features/*/module.ts` needs exactly one help entry with that `path`, and help
links must point to menu screens; `helpContent.test.ts` fails the build otherwise. Add or update the
entry together with the screen.

## 11. Module documentation

Every business module has a guide with its business rules, lifecycle, accounting events and demo
rules, reports, ports, demo data and open points. Update it together with the code.

| Module | Guide |
|---|---|
| Underwriting | [`docs/modules/UNDERWRITING.md`](../modules/UNDERWRITING.md) |
| Claims | [`docs/modules/CLAIMS.md`](../modules/CLAIMS.md) |
| Reinsurance | [`docs/modules/REINSURANCE.md`](../modules/REINSURANCE.md) |
| Receivables & Banking | [`docs/modules/RECEIVABLES_AND_BANKING.md`](../modules/RECEIVABLES_AND_BANKING.md) |
| Payables & Cash | [`docs/modules/PAYABLES_AND_CASH.md`](../modules/PAYABLES_AND_CASH.md) |
| Assets & Investments | [`docs/modules/ASSETS_AND_INVESTMENTS.md`](../modules/ASSETS_AND_INVESTMENTS.md) |
| Operations (BRD-2) | [`docs/modules/OPERATIONS.md`](../modules/OPERATIONS.md) |
| Planning & Closing | [`docs/development/PLANNING_AND_CLOSING.md`](PLANNING_AND_CLOSING.md) |
| Actuarial Reserves | [`docs/modules/ACTUARIAL_RESERVES.md`](../modules/ACTUARIAL_RESERVES.md) |
| Tax & Statutory | [`docs/modules/TAX_AND_STATUTORY.md`](../modules/TAX_AND_STATUTORY.md) |
| Broking (BDOI New Business) | [`docs/architecture/BROKING_ARCHITECTURE.md`](../architecture/BROKING_ARCHITECTURE.md) |
