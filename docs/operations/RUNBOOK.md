# Operations Runbook

Audience: production deployment and support teams.

## 1. Components

| Component | Image | Port | Health |
|---|---|---|---|
| `bibs-web` (Spring Boot, Java 21, role `web`): screens and user APIs | `bibs-backend` | 8443 (HTTPS); 9090 management | `/livez`, `/readyz` on 8443; `/actuator/health/**` and `/actuator/prometheus` on 9090 |
| `bibs-jobs` (same image, role `jobs`): scheduled and batch jobs | `bibs-backend` | 8443 (probes only); 9090 management | same |
| `bibs-integration` (same image, role `integration`): outbox relay, Kafka consumers, inbound files, `/integration/**` | `bibs-backend` | 8443 (HTTPS); 9090 management | same |
| `bibs-frontend` (nginx + SPA) | `bibs-frontend` | 8443 (HTTPS) | `/healthz` |
| PostgreSQL 16 | managed service | 5432 (TLS) | – |

Kubernetes layout, edge and TLS: [`DEPLOYMENT.md`](DEPLOYMENT.md). With `docker compose` one backend container runs
every role (`all`) over plain HTTP on 8080.

The backend is stateless (JWT); run ≥ 2 `bibs-web` replicas behind the load balancer. Database migrations (Flyway)
run automatically at start-up; with several replicas, Flyway's lock makes them wait for each other.
Out-of-order migrations are enabled because versions are allocated in per-module ranges: an upgrade may
apply, for example, V27 on a database already at V975. `flyway_schema_history` records the actual order.

## 2. First installation

1. Create the database and a user owning it.
2. Create the secret `bibs-backend-secrets` (see `deploy/k8s/base/secrets.example.yaml`) including
   `BROKERVERSE_ADMIN_INITIAL_PASSWORD`, and the ConfigMap `rds-ca-bundle` (DEPLOYMENT.md).
3. Deploy the overlay of the environment (`kubectl apply -k deploy/k8s/overlays/<uat|prod>`, DEPLOYMENT.md), or
   `docker compose` for a single host.
4. Sign in as `sysadmin`: the initial password must be changed at once, and the authenticator app is
   enrolled (the second factor is required of privileged users, `MFA_POLICY` = `PRIVILEGED`); keep the
   recovery codes in the safe. Remove `BROKERVERSE_ADMIN_INITIAL_PASSWORD` from the secret.
5. Set up in this order (each item is authorized by a second user – maker-checker):
   company → branches → currencies & rates → chart of accounts → dimensions → fiscal year & open
   periods → parties → accounting rules → users & roles.

## 3. Releases

- The CI pipeline (`.gitlab-ci.yml`) must be green: all quality gates and tests.
- Deploy with a rolling update (`maxUnavailable: 0`). New migrations are backward compatible with
  the previous release by rule (expand → migrate → contract across releases).
- Rollback: redeploy the previous image. Never roll back a database migration by hand; fix
  forward with a new migration.
- Seed environments only (never production): when the release notes say that seed scripts (`db/seed`) changed
  only in comments, run `flyway repair` once against the seed database, or recreate it, before starting the new
  release; otherwise Flyway refuses the start on the changed checksums. Current note: the SIT/UAT password left the
  seed script comments (CONFIGURATION.md, "Seed data").

## 4. Monitoring

- Metrics: `/actuator/prometheus` on the management port (`BROKERVERSE_MANAGEMENT_PORT`, 9090 in
  Kubernetes; annotations `prometheus.io/scrape|port|path` on the pods). No token is needed there: the
  NetworkPolicy `allow-monitoring` admits only the `monitoring` namespace to port 9090 and the load
  balancer never publishes it. Without a separate management port (docker compose) the metrics need
  a user holding `METRICS_VIEW` (no role holds it as delivered). JVM, HTTP, Hikari pool,
  `brokerverse_journals_posted_total` and `brokerverse_security_store_failures_total{store}`.
- Alert rule: any increase of `brokerverse_security_store_failures_total` (the token denylist, the
  session log or the rate limit counters cannot be read) is critical: sign-ins are checked against the
  database or refused (HTTP 503 `SIGN_IN_CHECK_UNAVAILABLE`). The application also raises the alert
  `SECURITY_STORE_UNAVAILABLE`. Check Valkey (`BROKERVERSE_VALKEY_*`) and the database; nothing to
  restart in the application once the store answers.
- Logs: one line per event, ISO timestamps; CR/LF in messages are neutralised. Alert on `ERROR`.
- Every HTTP response carries `X-Correlation-Id` (the caller's value when valid, else generated); the
  same id is in the envelope of the integration events the request published (`evt_outbox`,
  `evt_archive`).
- `/actuator/health` includes Valkey (component `redis`, named after the client) when
  `BROKERVERSE_VALKEY_ENABLED=true`. Watch `evt_outbox` rows in
  `PENDING` / `FAILED` and new dead letters (Administration → Integration Events).
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
| Unlock a user (locked after `LOGIN_MAX_FAILED_ATTEMPTS` failed sign-ins or second-factor codes) | Administration → Users → Unlock |
| A user lost the phone of the authenticator app | Administration → Second Factor → Request Reset (reason); a second administrator (`MFA_RESET_APPROVE`) approves; the user enrols again at the next sign-in. A recovery code signs the user in meanwhile |
| Single sign-on refused (`/sso/callback?error=...`) | `SSO_NOT_LINKED`: no active BrokerVerse user with the provider's user name (check `BROKERVERSE_SSO_USERNAME_CLAIM`); `SSO_INVALID`: signature, audience, time or state refused (check the provider certificate, the clocks, `BROKERVERSE_SSO_BASE_URL`); `SSO_PROVIDER_ERROR`: the provider's token endpoint refused (client secret). The log line of `SsoSignIn` gives the detail. The break-glass administrators sign in with their password meanwhile |
| Users signed out with "session ended" after a renewal (`TOKEN_REUSED` in the session list) | A replaced refresh token came back: a copied browser profile or a stolen cookie. Ask the user, check the sessions of the user; sign-in works again at once |
| Reset password | Administration → Users (API `POST /api/v1/admin/users/{id}/reset-password`) |
| Who changed what | Administration → Audit Trail, or report `CTL-AUDIT` |
| Failed accounting events | Accounting Engine → Event Register (status FAILED) |
| Period close blocked | Journals filtered by status + period dates |
| Integration events stuck or failed (Kafka), dead letters | Administration → Integration Events; runbook in [`PLATFORM_CACHE_AND_EVENTS.md`](../architecture/PLATFORM_CACHE_AND_EVENTS.md) §5 |
| Reference data changed by SQL not visible (cache) | `POST /api/v1/admin/caches/{name}/clear` (§5.3 of the same document) |
| A job shows `SKIPPED_LOCKED` | Normal with several replicas: another instance ran it (job lock, §5.4) |

## 8. Security settings of a deployment

- `BROKERVERSE_ENVIRONMENT` names the environment (`sit`, `uat`, `training`, `preprod`, `production`).
  Every environment except `local` refuses to start without its secrets and with development values
  (the log line lists every problem). Secrets and keys: [`CONFIGURATION.md`](CONFIGURATION.md)
  "Authentication and session security".
- Rotating the key of the second factor: set the new key in `BROKERVERSE_MFA_ENCRYPTION_KEY` and the
  old one in `BROKERVERSE_MFA_PREVIOUS_ENCRYPTION_KEY`, restart; each secret is re-encrypted at its
  next use. Remove the previous key once every enrolled user has signed in (or reset those who have
  not).
- Rotating the JWT key signs every user out once (access tokens are refused; the refresh cookies still
  renew them, so users continue working after one renewal).

## 9. Promote configuration between environments

Configuration (masters, configuration, rules and validations, never transactions) moves from one environment to the
next as a signed package: development → SIT → UAT → production. Guide:
[`CONFIG_PROMOTION.md`](../modules/CONFIG_PROMOTION.md).

Before the first promotion:

- Set the same `BROKERVERSE_CONFIG_PROMOTION_SIGNING_KEY` (from the secret store) in every environment of the path;
  Environment Overrides shows the key identifier, which must be the same on both sides.
- In production, set the change window `CONFIG_PROMOTION_PRODUCTION_WINDOW` on System Parameters and give the
  Configuration Release Approver profile to the approvers (never to the System Administrators: rule SOD-CFG-01).
- Source and target run the same release (the import refuses datasets whose fields differ).

Each promotion:

1. **Source** - System Administration › Configuration Promotion › Export Configuration: keep the default selection
   (or the datasets of the change, or only the datasets changed since a baseline), give the purpose, export and
   download the package. Users are included only when asked.
2. **Target** - Import Configuration › Upload a Package with the change request number and the reason (mandatory in
   production). Read the findings: blockers must be solved first (a missing reference, an item in use that would be
   deactivated); warnings are read and accepted. Open each dataset to see the differences field by field; clear
   Import to leave a dataset out, tick Deactivate for the items only in the target that must go, Check Again.
3. **Submit** for approval. The Configuration Release Approver opens the import (notification or My Approvals),
   reviews the same differences and approves, which applies it - only inside the change window in production, and
   not while a period close or a batch job runs (`CONFIG_IMPORT_JOB_RUNNING`: try again after the run).
4. **Check** the reconciliation of each dataset (all Matched) and the audit trail. Mark a baseline after an accepted
   promotion (Baselines and Drift) so the next package can be incremental and drift is visible.

When something goes wrong:

| Symptom | Action |
|---|---|
| `CONFIG_PACKAGE_INVALID` (signature or checksum) | The package was changed or signed with another key: export again; check that both environments have the same key identifier |
| Finding "has other fields here than in the source environment" (import not compatible) | Source and target run different releases: promote after the release reaches the target |
| `CONFIG_OUTSIDE_CHANGE_WINDOW` | Production only: approve inside the change window set in `CONFIG_PROMOTION_PRODUCTION_WINDOW` |
| Import Failed | Nothing of it was kept (one transaction). Read the message, correct the cause in the source or the target, Check Again |
| Wrong configuration applied | Import Configuration › the applied import › Roll Back: prepares an import of the snapshot taken before the apply, approved like any import |
| Drift found in production | Baselines and Drift › the dataset: shows what changed since the baseline; promote the change through the path or correct it on its screen |

A deployment pipeline uses the same API with a service user (`CONFIG_IMPORT_PREPARE`): it uploads with
`pipeline=true`, reads the findings and submits; outside production, when `CONFIG_PROMOTION_PIPELINE_APPLY` is true,
it may apply its own clean dry run (`apply=true`).
