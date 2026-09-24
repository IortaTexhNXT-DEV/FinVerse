# Configuration Reference

All settings are environment variables (12-factor). Defaults in `backend/src/main/resources/application.yml`
are for local development only.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | – | `prod` in production. `demo` loads demo data (never in production). |
| `BROKERVERSE_DB_URL` | yes | `jdbc:postgresql://localhost:5432/brokerverse` | JDBC URL (use `sslmode=require`). |
| `BROKERVERSE_DB_USER` | yes | `brokerverse` | Database user (owner of the schema; Flyway migrates on start). |
| `BROKERVERSE_DB_PASSWORD` | yes | `brokerverse` | Database password (from secret store). |
| `BROKERVERSE_DB_POOL_SIZE` | no | `20` | Hikari maximum pool size per instance. |
| `BROKERVERSE_JWT_SECRET` | yes | dev value | HMAC key for access tokens, ≥ 32 random characters. Rotating it signs everyone out. |
| `BROKERVERSE_TOKEN_VALIDITY` | no | `PT8H` | Access token lifetime (ISO-8601 duration). |
| `BROKERVERSE_ALLOWED_ORIGINS` | yes | `http://localhost:5173` | Comma separated browser origins allowed by CORS. |
| `BROKERVERSE_ADMIN_USERNAME` | first start | `sysadmin` | Initial administrator (created only when no user exists). |
| `BROKERVERSE_ADMIN_INITIAL_PASSWORD` | first start | – | Initial administrator password; remove after first login. |
| `BROKERVERSE_PORT` | no | `8080` | HTTP port. |
| `BROKERVERSE_JOB_RECURRING_CRON` | no | `0 0 1 * * *` | Spring cron (UTC) of `RECURRING_JOURNALS`: generates the due recurring and accrual journals. |
| `BROKERVERSE_JOB_ALERTS_CRON` | no | `0 30 1 * * *` | Spring cron (UTC) of `ALERT_DAILY_CHECKS`: evaluates the scheduled exception codes. |
| `BROKERVERSE_JOB_RESERVE_VALUATION_CRON` | no | `-` (off) | Spring cron (UTC) of the monthly actuarial reserve valuation of the previous month. |
| `BROKERVERSE_JOB_RI_ALLOCATION_CRON` | no | `-` (off) | Spring cron (UTC) of the scheduled reinsurance allocation run; the run can always be started on demand. |
| `BROKERVERSE_JOB_PDC_ISSUED_DUE_CRON` | no | `0 15 0 * * *` | Spring cron (UTC) of `PDC_ISSUED_DUE`: post-dated cheques issued whose cheque date is reached become DUE (status only, no posting). Replaces the former `brokerverse.payables.pdc-due-cron`. |
| `BROKERVERSE_JOB_QUOTATION_EXPIRY_CRON` | no | `0 45 0 * * *` | Spring cron (UTC) of `QUOTATION_EXPIRY`: open quotations of every active company past their validity become EXPIRED. |
| `BROKERVERSE_BACKEND_HOST` (frontend container) | yes | `backend` | Host name of the backend service for the `/api` proxy. |

Every background job is a `ManagedJob` listed on *Administration › Scheduled Jobs* with its next
run, run history and "Run now". A cron of `-` disables the schedule (manual runs only); cron
expressions have six fields (second minute hour day month weekday) and are evaluated in UTC, and
the business date of a scheduled run is the UTC date.

Dashboard KPI mapping (optional, `application.yml` or env `BROKERVERSE_DASHBOARD_CASH_GROUPS_0` …):
`brokerverse.dashboard.cash-groups`, `receivable-groups`, `reserve-groups` list chart-of-accounts
statement lines (`report_group`) used for the cash, receivables and technical reserve tiles.
