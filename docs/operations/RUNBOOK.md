# Operations Runbook

Audience: production deployment and support teams.

## 1. Components

| Component | Image | Port | Health |
|---|---|---|---|
| Backend (Spring Boot, Java 21) | `brokerverse-backend` | 8080 | `/actuator/health/liveness`, `/actuator/health/readiness` |
| Frontend (nginx + SPA, proxies `/api`) | `brokerverse-frontend` | 8080 | `/healthz` |
| PostgreSQL 16 | managed service | 5432 | – |

The backend is stateless (JWT); run ≥ 2 replicas behind the service. Database migrations (Flyway)
run automatically at start-up; with several replicas, Flyway's lock makes them wait for each other.
Out-of-order migrations are enabled because versions are allocated in per-module ranges: an upgrade may
apply, for example, V27 on a database already at V975. `flyway_schema_history` records the actual order.

## 2. First installation

1. Create the database and a user owning it.
2. Create the secret (see `deploy/k8s/brokerverse-secrets.example.yaml`) including
   `BROKERVERSE_ADMIN_INITIAL_PASSWORD`.
3. Deploy `deploy/k8s/brokerverse.yaml` (or `docker compose` for a single host).
4. Sign in as `sysadmin`, change the password, remove `BROKERVERSE_ADMIN_INITIAL_PASSWORD`.
5. Set up in this order (each item is authorized by a second user – maker-checker):
   company → branches → currencies & rates → chart of accounts → dimensions → fiscal year & open
   periods → parties → accounting rules → users & roles.

## 3. Releases

- CI (`.github/workflows/ci.yml`) must be green: all quality gates and tests.
- Deploy with a rolling update (`maxUnavailable: 0`). New migrations are backward compatible with
  the previous release by rule (expand → migrate → contract across releases).
- Rollback: redeploy the previous image. Never roll back a database migration by hand; fix
  forward with a new migration.

## 4. Monitoring

- Metrics: `/actuator/prometheus` (requires `SYSTEM_PARAMETER_MANAGE`; scrape with a service
  account token) – JVM, HTTP, Hikari pool, and `brokerverse_journals_posted_total`.
- Logs: one line per event, ISO timestamps; CR/LF in messages are neutralised. Alert on `ERROR`.
- Every unexpected error returns HTTP 500 with `code=INTERNAL_ERROR` and a `reference` UUID that is
  also written to the log line – ask users for it and search the logs.

## 5. Business error codes (HTTP 422)

| Code | Meaning | Typical action |
|---|---|---|
| `PERIOD_NOT_OPEN` / `NO_PERIOD` | Posting date is in a closed/future period | Open the period or use an open date |
| `JOURNAL_INVALID` | One or more account/dimension controls failed (message lists them) | Correct the voucher |
| `UNBALANCED_JOURNAL` | Debits ≠ credits | Correct amounts |
| `MAKER_CHECKER_VIOLATION` | Same user tried to authorize own work | Another user must authorize |
| `AUTHORIZATION_LIMIT_EXCEEDED` | Amount above the user's limit | Higher-limit authorizer |
| `NO_ACCOUNTING_RULE` | No active rule for an event/LOB/currency | Configure and authorize a rule |
| `MISSING_ACCOUNT_ROLE` | Event did not supply an `@ROLE` account | Check the module setup (e.g. bank account) |
| `RATE_NOT_FOUND` | No exchange rate on/before date | Maintain the rate |
| `PERIOD_CLOSE_BLOCKED` | Pending journals in the period | Post or cancel them |
| `INACTIVE_*` | Master record not authorized/active | Authorize the record |

## 6. Backup and recovery

- Database: daily full backup + WAL archiving (point-in-time recovery). Test restores quarterly.
- The ledger (`gl_ledger_entry`) is insert-only and protected by a trigger; corrections are always
  reversal journals, so the audit trail is never lost.
- The audit trail (`audit_log`) is insert-only as well: triggers (V26) reject `UPDATE`, `DELETE`
  and `TRUNCATE`. Archiving old entries is a DBA task: the table owner disables the triggers in a
  recorded change, copies and removes the rows, and re-enables them.
- `gl_daily_balance` can be rebuilt from `gl_ledger_entry` if ever in doubt:
  ```sql
  begin;
  truncate gl_daily_balance;
  insert into gl_daily_balance
  select company_id, branch_id, account_id, currency, value_date,
         sum(debit_fc), sum(credit_fc), sum(debit_base), sum(credit_base)
  from gl_ledger_entry group by 1, 2, 3, 4, 5;
  commit;
  ```

## 7. Routine support tasks

| Task | Where |
|---|---|
| Unlock a user (locked after 5 failed logins) | Administration → Users → Unlock |
| Reset password | Administration → Users (API `POST /api/v1/admin/users/{id}/reset-password`) |
| Who changed what | Administration → Audit Trail, or report `CTL-AUDIT` |
| Failed accounting events | Accounting Engine → Event Register (status FAILED) |
| Period close blocked | Journals filtered by status + period dates |
