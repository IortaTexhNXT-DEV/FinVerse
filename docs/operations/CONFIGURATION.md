# Configuration Reference

All settings are environment variables (12-factor). Defaults in `backend/src/main/resources/application.yml`
are for local development only.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | – | `prod` in production. `demo` loads demo data (never in production). |
| `FINVERSE_DB_URL` | yes | `jdbc:postgresql://localhost:5432/finverse` | JDBC URL (use `sslmode=require`). |
| `FINVERSE_DB_USER` | yes | `finverse` | Database user (owner of the schema; Flyway migrates on start). |
| `FINVERSE_DB_PASSWORD` | yes | `finverse` | Database password (from secret store). |
| `FINVERSE_DB_POOL_SIZE` | no | `20` | Hikari maximum pool size per instance. |
| `FINVERSE_JWT_SECRET` | yes | dev value | HMAC key for access tokens, ≥ 32 random characters. Rotating it signs everyone out. |
| `FINVERSE_TOKEN_VALIDITY` | no | `PT8H` | Access token lifetime (ISO-8601 duration). |
| `FINVERSE_ALLOWED_ORIGINS` | yes | `http://localhost:5173` | Comma separated browser origins allowed by CORS. |
| `FINVERSE_ADMIN_USERNAME` | first start | `sysadmin` | Initial administrator (created only when no user exists). |
| `FINVERSE_ADMIN_INITIAL_PASSWORD` | first start | – | Initial administrator password; remove after first login. |
| `FINVERSE_PORT` | no | `8080` | HTTP port. |
| `FINVERSE_JOB_RECURRING_CRON` | no | `0 0 1 * * *` | Spring cron (UTC) of `RECURRING_JOURNALS`: generates the due recurring and accrual journals. |
| `FINVERSE_JOB_ALERTS_CRON` | no | `0 30 1 * * *` | Spring cron (UTC) of `ALERT_DAILY_CHECKS`: evaluates the scheduled exception codes. |
| `FINVERSE_JOB_RESERVE_VALUATION_CRON` | no | `-` (off) | Spring cron (UTC) of the monthly actuarial reserve valuation of the previous month. |
| `FINVERSE_JOB_RI_ALLOCATION_CRON` | no | `-` (off) | Spring cron (UTC) of the scheduled reinsurance allocation run; the run can always be started on demand. |
| `FINVERSE_JOB_PDC_ISSUED_DUE_CRON` | no | `0 15 0 * * *` | Spring cron (UTC) of `PDC_ISSUED_DUE`: post-dated cheques issued whose cheque date is reached become DUE (status only, no posting). Replaces the former `finverse.payables.pdc-due-cron`. |
| `FINVERSE_BACKEND_HOST` (frontend container) | yes | `backend` | Host name of the backend service for the `/api` proxy. |

Every background job is a `ManagedJob` listed on *Administration › Scheduled Jobs* with its next
run, run history and "Run now". A cron of `-` disables the schedule (manual runs only); cron
expressions have six fields (second minute hour day month weekday) and are evaluated in UTC, and
the business date of a scheduled run is the UTC date.

Dashboard KPI mapping (optional, `application.yml` or env `FINVERSE_DASHBOARD_CASH_GROUPS_0` …):
`finverse.dashboard.cash-groups`, `receivable-groups`, `reserve-groups` list chart-of-accounts
statement lines (`report_group`) used for the cash, receivables and technical reserve tiles.
