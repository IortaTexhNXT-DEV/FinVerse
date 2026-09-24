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
| `BROKERVERSE_JOB_KYC_REVIEW_DUE_CRON` | no | `0 0 2 1 * *` | Spring cron (UTC) of `KYC_REVIEW_DUE` (monthly, BRNB.110): verified client KYC past its review date becomes EXPIRED and the holders of `CLIENT_MAINTAIN` are notified of the number of non-bank clients due. |
| `BROKERVERSE_JOB_RETENTION_REVIEW_CRON` | no | `0 0 3 2 * *` | Spring cron (UTC) of `RETENTION_REVIEW` (monthly, BRNB.106): counts the records eligible under each data retention rule. Nothing is archived or deleted. |
| `BROKERVERSE_JOB_QUOTATION_REQUEST_INTAKE_CRON` | no | `-` (off) | Spring cron (UTC) of `QUOTATION_REQUEST_INTAKE` (BRNB.023): stores the quotation requests of the connected source systems (`QuotationRequestSource` implementations) in the request inbox. No source system is connected until the HLS interface is specified (Q11); requests are captured manually or uploaded with the `QUOTATION_REQUEST` bulk handler. |
| `BROKERVERSE_JOB_BOOKING_BATCH_CRON` | no | `0 0 12 * * *` | Spring cron (UTC) of `BOOKING_BATCH` (daily 20:00 PHT, BRNB.036/112): books the accounts queued for booking (users, placement's "For Booking", auto-book rules) and the policy years of multi-year accounts that have started; one batch run per company with a result per account. |
| `BROKERVERSE_JOB_MAIL_DISPATCH_CRON` | no | `0 */2 * * * *` | Spring cron (UTC) of `MAIL_DISPATCH`: delivers queued e-mails and retries failed attempts (up to the `MAIL_MAX_ATTEMPTS` business parameter). |
| `BROKERVERSE_JOB_PAYMENT_CONFIRMATION_SWEEP_CRON` | no | `0 0 * * * *` | Spring cron (UTC) of `PAYMENT_CONFIRMATION_SWEEP` (hourly): asks every payment confirmation source (today the confirmed payment reports) about the accounts awaiting payment and opens their payment gate. |
| `BROKERVERSE_JOB_HOLD_COVER_EXPIRY_CRON` | no | `0 30 0 * * *` | Spring cron (UTC) of `HOLD_COVER_EXPIRY` (daily, BRNB.072/103): notifies the holders of `PLACEMENT_MANAGE` of hold covers expiring within `HOLD_COVER_ALERT_DAYS` and expires the lapsed ones. |
| `BROKERVERSE_JOB_OPS_INVOICE_FEED_REPLAY_CRON` | no | `-` (off) | Spring cron (UTC) of `OPS_INVOICE_FEED_REPLAY` (Operations, `opsledger`): copies into the Operations invoice ledger every booked invoice missing from it, as a logged `OPS_INVOICE_FEED` run. New bookings are copied by the feed listener; the job (or *Operations › Interfaces › Replay*) recovers gaps. Demo start-up runs it once. |
| `BROKERVERSE_JOB_PREBOOKED_REMATCH_CRON` | no | `0 0 */2 * * *` | Spring cron (UTC) of `PREBOOKED_REMATCH` (cashiering, CSHID.020): re-matches payments waiting for a pre-booked account; also started by an invoice entering the ledger. Property `brokerverse.jobs.prebooked-rematch-cron`. |
| `BROKERVERSE_JOB_PAYMENT_AUTOMATCH_CRON` | no | `0 30 * * * *` | Spring cron (UTC) of `PAYMENT_AUTOMATCH` (cashiering, CSHID.008 item 3): automatic matching of uploaded payments, hourly and after each upload. Property `brokerverse.jobs.payment-automatch-cron`. |
| `BROKERVERSE_JOB_PDC_MATURITY_CRON` | no | `0 30 0 * * *` | Spring cron (UTC) of `PDC_MATURITY` (cashiering, CSHID.008 item 4e): warehoused post-dated checks reaching maturity become payments. Property `brokerverse.jobs.pdc-maturity-cron`. |
| `BROKERVERSE_JOB_MINIMAL_BALANCE_SWEEP_CRON` | no | `0 0 20 * * *` | Spring cron (UTC) of `MINIMAL_BALANCE_SWEEP` (cashiering, CSHID.016): reverses premium receivable balances up to `MIN_BALANCE_AUTO_MAX`. Property `brokerverse.jobs.minimal-balance-sweep-cron`. |
| `BROKERVERSE_JOB_REMITTANCE_EXTRACTION_CRON` | no | `0 0 12 * * *` | Spring cron (UTC) of `REMITTANCE_EXTRACTION` (remittance, RMTID.001/003/005): off-peak extraction, 20:00 PHT. Property `brokerverse.jobs.remittance-extraction-cron`. |
| `BROKERVERSE_JOB_HOLD_EXPIRY_CRON` | no | `0 15 0 * * *` | Spring cron (UTC) of `HOLD_EXPIRY` (remittance, RMTID.021): releases holds past their hold date. Property `brokerverse.jobs.hold-expiry-cron`. |
| `BROKERVERSE_JOB_PRODUCTION_EXTRACT_CRON` | no | `0 0 1 * * *` | Spring cron (UTC) of `PRODUCTION_EXTRACT` (prodrecon, PRCID.001): daily check of the insurer extraction schedules (holiday roll). Property `brokerverse.jobs.production-extract-cron`. |
| `BROKERVERSE_JOB_RECON_AUTOMATCH_CRON` | no | `-` (off) | Spring cron (UTC) of `RECON_AUTOMATCH` (prodrecon, PRCID.025): manual until BDOI gives the frequency (OQ30). Property `brokerverse.jobs.recon-automatch-cron`. |
| `BROKERVERSE_JOB_ADJ_DAILY_REPORT_CRON` | no | `0 0 10 * * *` | Spring cron (UTC) of `ADJ_DAILY_REPORT` (adjustment, ADJID.016): daily adjustment report, 18:00 PHT. Property `brokerverse.jobs.adj-daily-report-cron`. |
| `BROKERVERSE_JOB_DP_FEEDBACK_SLA_CRON` | no | `0 30 1 * * *` | Spring cron (UTC) of `DP_FEEDBACK_SLA` (commission, CMRID.011): flags insurer feedback on direct payment billings beyond `CMR_FEEDBACK_WORKING_DAYS`. Property `brokerverse.jobs.dp-feedback-sla-cron`. |
| `BROKERVERSE_MAIL_ENABLED` | no | `false` | `true` delivers e-mails through the SMTP server below; `false` records them in the outbox as *simulated* (demo, test, UAT without mail). Addresses ending in `.invalid` are always rejected by the simulated transport. |
| `BROKERVERSE_MAIL_DISPATCH_ON_COMMIT` | no | `true` | Deliver right after the business transaction commits; `false` leaves delivery to the `MAIL_DISPATCH` job only. |
| `MAIL_HOST` / `MAIL_PORT` | when mail enabled | `localhost` / `587` | SMTP server. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | when the server requires it | — | SMTP credentials (secret: supply from the vault, never in files). |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | no | `true` / `true` | SMTP authentication and STARTTLS. |
| `BROKERVERSE_BACKEND_HOST` (frontend container) | yes | `backend` | Host name of the backend service for the `/api` proxy. |

Every background job is a `ManagedJob` listed on *Administration › Scheduled Jobs* with its next
run, run history and "Run now". A cron of `-` disables the schedule (manual runs only); cron
expressions have six fields (second minute hour day month weekday) and are evaluated in UTC, and
the business date of a scheduled run is the UTC date.

Dashboard KPI mapping (optional, `application.yml` or env `BROKERVERSE_DASHBOARD_CASH_GROUPS_0` …):
`brokerverse.dashboard.cash-groups`, `receivable-groups`, `reserve-groups` list chart-of-accounts
statement lines (`report_group`) used for the cash, receivables and technical reserve tiles.
