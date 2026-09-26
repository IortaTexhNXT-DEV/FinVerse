# Configuration Reference

All settings are environment variables (12-factor). Defaults in `backend/src/main/resources/application.yml`
are for local development only.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | – | `prod` in production. `seed` loads the seed data of SIT, UAT and training (never in production: the start is refused, see "Production start-up safeguards"). |
| `BROKERVERSE_ENVIRONMENT` | yes | `local` (`seed` with the seed profile) | `brokerverse.environment`: `local`, `sit`, `uat`, `training`, `preprod` or `production`. `production` (set by the `prod` profile) turns on the start-up safeguards. |
| `BROKERVERSE_DB_URL` | yes | `jdbc:postgresql://localhost:5432/brokerverse`; none with `prod` | JDBC URL (use `sslmode=require`). |
| `BROKERVERSE_DB_USER` | yes | `brokerverse`; none with `prod` | Database user (owner of the schema; Flyway migrates on start). |
| `BROKERVERSE_DB_PASSWORD` | yes | none (a local value with the `seed` profile only) | Database password (from secret store). |
| `BROKERVERSE_DB_POOL_SIZE` | no | `20` | Hikari maximum pool size per instance. |
| `BROKERVERSE_JWT_SECRET` | yes | none (a local value with the `seed` and test profiles only) | HMAC key for access tokens, ≥ 32 random characters. Rotating it signs everyone out. |
| `BROKERVERSE_TOKEN_VALIDITY` | no | `PT8H` | Access token lifetime (ISO-8601 duration). |
| `BROKERVERSE_ALLOWED_ORIGINS` | yes | `http://localhost:5173` | Comma separated browser origins allowed by CORS. |
| `BROKERVERSE_MAX_FAILED_ATTEMPTS` | no | `5` | Consecutive failed logins that lock an account when the business parameter `LOGIN_MAX_FAILED_ATTEMPTS` is missing. The parameter (seeded with 3, BDOI NFR, CQ23) wins; administrators change it on *Administration › Parameters*. Property `brokerverse.security.max-failed-attempts`. |
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
| `BROKERVERSE_JOB_OPS_INVOICE_FEED_REPLAY_CRON` | no | `-` (off) | Spring cron (UTC) of `OPS_INVOICE_FEED_REPLAY` (Operations, `opsledger`): copies into the Operations invoice ledger every booked invoice missing from it, as a logged `OPS_INVOICE_FEED` run. New bookings are copied by the feed listener; the job (or *Operations › Interfaces › Replay*) recovers gaps. Seed start-up runs it once. |
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
| `BROKERVERSE_JOB_PACKAGE_EXPIRY_CRON` | no | `0 0 17 * * *` | Spring cron (UTC) of `PACKAGE_EXPIRY_MONITOR` (Product Maintenance, BRPM.006/017), daily 01:00 PHT: moves package versions to SUPERSEDED / EXPIRED, raises `PACKAGE_EXPIRING` at `PACKAGE_EXPIRY_NOTICE_DAYS` and at 30 / 7 days, and drafts RENEW package requests when `PACKAGE_RENEWAL_AUTODRAFT` is true. Note that the business date of the 17:00 UTC run is the PHT date of the previous day. The catalog job `PACKAGE_VERSION_LIFECYCLE` (supersedes and expires package versions) runs on the same schedule. Property `brokerverse.jobs.package-expiry-cron`. |
| `BROKERVERSE_JOB_CLX_DAILY_REFRESH_CRON` | no | `0 15 14 * * *` | Spring cron (UTC) of `CLX_DAILY_REFRESH` (Collections, BRCLXN.001-015, 046, 052, 053; 22:15 PHT, after the 22:00 EOD: incremental refresh of the collection worklist from the invoice ledger (new, updated, completed and excluded items, unit head, aging and bracket), installment allocation and overdue flags, temporary-assignment revert and default assignment by rule). Property `brokerverse.jobs.clx-daily-refresh-cron`. |
| `BROKERVERSE_JOB_CLX_PROMISE_CHECK_CRON` | no | `0 45 14 * * *` | Spring cron (UTC) of `CLX_PROMISE_CHECK` (Collections, BRCLXN.055; 22:45 PHT: evaluates the promises to pay due yesterday or earlier (kept, partially kept, broken after `CLX_PROMISE_GRACE_DAYS`)). Property `brokerverse.jobs.clx-promise-check-cron`. |
| `BROKERVERSE_JOB_CLX_ESCALATION_CRON` | no | `0 0 15 * * *` | Spring cron (UTC) of `CLX_ESCALATION` (Collections, BRCLXN.049; 23:00 PHT: applies the escalation rules, one case per account, rule and period). Property `brokerverse.jobs.clx-escalation-cron`. |
| `BROKERVERSE_JOB_CLX_DAILY_FILES_CRON` | no | `0 30 14 * * *` | Spring cron (UTC) of `CLX_DAILY_FILES` (Collections, BRCLXN.045; 22:30 PHT: Outstanding PR List and Full Production Report as scheduled files). Property `brokerverse.jobs.clx-daily-files-cron`. |
| `BROKERVERSE_JOB_CLX_APPLICATION_FILE_CRON` | no | `0 0 21 * * *` | Spring cron (UTC) of `CLX_APPLICATION_FILE` (Collections, BRCLXN.041/042; 05:00 PHT: "For Application To Invoice" text file of the previous day's requests through `FileDropPort` (FS04 parked)). Property `brokerverse.jobs.clx-application-file-cron`. |
| `BROKERVERSE_JOB_CLX_WEEKLY_FILES_CRON` | no | `0 30 14 * * FRI` | Spring cron (UTC) of `CLX_WEEKLY_FILES` (Collections, BRCLXN.028/029; Friday 22:30 PHT: weekly DP PR / PR 2307 for reversal per unit and branch, available Monday 08:00 PHT). Property `brokerverse.jobs.clx-weekly-files-cron`. |
| `BROKERVERSE_JOB_CLX_MONTHLY_FILES_CRON` | no | `0 0 21 * * *` | Spring cron (UTC) of `CLX_MONTHLY_FILES` (Collections, BRCLXN.024-027; runs daily at 05:00 PHT and generates the monthly DP PR / PR 2307 for reversal of the previous month on the first working day only). Property `brokerverse.jobs.clx-monthly-files-cron`. |
| `BROKERVERSE_JOB_JOURNAL_AUTO_REVERSAL_CRON` | no | `0 5 16 * * *` | Spring cron (UTC) of `JOURNAL_AUTO_REVERSAL` (journal, FRBS 2.8.1; 00:05 PHT: posts the reversal of accrual journals whose "reverse on" date is reached). Property `brokerverse.jobs.journal-auto-reversal-cron`. |
| `BROKERVERSE_JOB_GL_PERIOD_CLOSE_CRON` | no | `0 */15 * * * *` | Spring cron (UTC) of `GL_PERIOD_CLOSE` (closing, FRBS 2.6.0; every 15 minutes: runs the month-end or year-end close scheduled by FRBS in `acc_period_close_schedule`, or notifies what blocks it (`GL_CLOSE_FAILED`)). Property `brokerverse.jobs.gl-period-close-cron`. |
| `BROKERVERSE_JOB_BROKING_BOOKS_CLOSE_CRON` | no | `0 0 15 L * *` | Spring cron (UTC) of `BROKING_BOOKS_CLOSE` (closing, FRBS 3.4.0/3.4.1; last day of the month at 23:00 PHT (`BROKING_CLOSE_TIME`): closes the broking books (module cut-off)). Property `brokerverse.jobs.broking-books-close-cron`. |
| `BROKERVERSE_JOB_BOOK_RATE_FROM_CLOSING_CRON` | no | `0 30 16 L * *` | Spring cron (UTC) of `BOOK_RATE_FROM_CLOSING` (currency, OQ08 / AQ03; 00:30 PHT on the first of the month: copies the previous month-end revaluation (CLOSING) rate as the BOOK rate when `OPS_BOOK_RATE_SOURCE` = CLOSING_PREV_MONTH). Property `brokerverse.jobs.book-rate-from-closing-cron`. |
| `BROKERVERSE_JOB_DISB_CHECK_STALE_CRON` | no | `0 20 16 * * *` | Spring cron (UTC) of `DISB_CHECK_STALE` (disbursement, DIS 3.26.2; 00:20 PHT: printed or released checks older than `DISB_STALE_DAYS` become stale (event `DISB_CHECK_STALE`, alert `DISB_CHECK_STALE`)). Property `brokerverse.jobs.disb-check-stale-cron`. |
| `BROKERVERSE_JOB_DISB_EOD_CONFIRMATION_CRON` | no | `-` (off) | Spring cron (UTC) of `DISB_EOD_CONFIRMATION` (disbursement, DIS 2.7.12; started by each end-of-day run (e-mail confirmations with the remittance schedule); no schedule by default). Property `brokerverse.jobs.disb-eod-confirmation-cron`. |
| `BROKERVERSE_JOB_DISB_EOD_REPORTS_CRON` | no | `-` (off) | Spring cron (UTC) of `DISB_EOD_REPORTS` (disbursement, DIS 3.28.0; started by each end-of-day run (EOD reports); no schedule by default). Property `brokerverse.jobs.disb-eod-reports-cron`. |
| `BROKERVERSE_JOB_ACSL_GL_SL_RECON_CRON` | no | `0 0 12 * * *` | Spring cron (UTC) of `ACSL_GL_SL_RECON` (acsl, ACSL 2.13.2; 20:00 PHT and at period end: reconciles every control account with its sub-ledger (`ACSL_GLSL_DIFFERENCE`)). Property `brokerverse.jobs.acsl-gl-sl-recon-cron`. |
| `BROKERVERSE_JOB_SCR_WATCHLIST_INGEST_CRON` | no | `0 0 17 * * *` | Spring cron (UTC) of `SCR_WATCHLIST_INGEST` (screening, SNSRP-201/202; 01:00 PHT, before business hours: pulls each active watchlist source through its `WatchlistFeed` adapter, logs the run and its failed records (`SCR_INGEST_FAILED`) and hands changed entries to delta screening). Property `brokerverse.jobs.scr-watchlist-ingest-cron`. |
| `BROKERVERSE_JOB_SCR_PERIODIC_SCREENING_CRON` | no | `0 30 17 * * *` | Spring cron (UTC) of `SCR_PERIODIC_SCREENING` (screening, SNSRP-301; 01:30 PHT: screens the clients of `SCR_SCREENING_SCOPE` against the entries changed since the last run; full rescreen on day `SCR_FULL_RESCREEN_DAY` of the month). Property `brokerverse.jobs.scr-periodic-screening-cron`. |
| `BROKERVERSE_JOB_SCR_SLA_MONITOR_CRON` | no | `0 0 * * * *` | Spring cron (UTC) of `SCR_SLA_MONITOR` (screening, SNSRP-405/802; hourly: case SLA reminders, breach flags (`SCR_SLA_BREACH`), escalation notices and missing-document reminders). Property `brokerverse.jobs.scr-sla-monitor-cron`. |
| `BROKERVERSE_JOB_SCR_INGEST_ERROR_DIGEST_CRON` | no | `0 0 23 * * SUN-THU` | Spring cron (UTC) of `SCR_INGEST_ERROR_DIGEST` (screening, SNSRP-202; 07:00 PHT Monday to Friday: e-mails the failed watchlist records to `SCR_INGEST_ALERT_RECIPIENTS`). Property `brokerverse.jobs.scr-ingest-error-digest-cron`. |
| `BROKERVERSE_JOB_UAM_EFFECTIVE_CHANGES_CRON` | no | `0 5 16 * * *` | Spring cron (UTC) of `UAM_EFFECTIVE_CHANGES` (nbadmin, UAM-NFR-14; 00:05 PHT: applies the SCHEDULED access requests whose effective date is today or earlier; a failure raises `UAM_SCHEDULED_APPLY_FAILED`). Property `brokerverse.jobs.uam-effective-changes-cron`. |
| `BROKERVERSE_JOB_PASSWORD_EXPIRY_NOTICE_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `PASSWORD_EXPIRY_NOTICE` (nbadmin, UAM-NFR-36; 06:00 PHT, `AUTH_MODE` LOCAL only: notifies the users whose password expires within 7 days of `PASSWORD_MAX_AGE_DAYS`, in the app and by e-mail, event `PASSWORD_EXPIRY_NOTICE`). Property `brokerverse.jobs.password-expiry-notice-cron`. |
| `BROKERVERSE_JOB_BCL_PREMIUM_RECHECK_CRON` | no | `0 30 21 * * *` | Spring cron (UTC) of `BCL_PREMIUM_RECHECK` (brokerclaims, BRCLM.001; 05:30 PHT: re-runs the premium check of open claims without an authorization code against the invoice ledger, as a safety net for missed `InvoiceMovementPosted` events; raises or resolves `BCL_UNPAID_PREMIUM_CLAIM`). Property `brokerverse.jobs.bcl-premium-recheck-cron`. |
| `BROKERVERSE_JOB_BCL_FOLLOW_UP_DUE_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `BCL_FOLLOW_UP_DUE` (brokerclaims, BRCLM.019/022/034; 06:00 PHT: notifies the handlers of claims whose next follow-up date or diary due date is today (`BCL_FOLLOW_UP_DUE`) and raises `BCL_FOLLOW_UP_OVERDUE` for past dates). Property `brokerverse.jobs.bcl-follow-up-due-cron`. |
| `BROKERVERSE_JOB_BCL_AGEING_ALERTS_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `BCL_AGEING_ALERTS` (brokerclaims, BRCLM.031, p.43; 06:00 PHT: raises `BCL_CLAIM_PAST_DUE` for outstanding claims older than `BCL_PAST_DUE_DAYS`). Property `brokerverse.jobs.bcl-ageing-alerts-cron`. |
| `BROKERVERSE_JOB_EB_RENEWAL_ADVICE_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `EB_RENEWAL_ADVICE` (eb, BRID-001/002; 06:00 PHT: opens the RENEWAL cycles of eligible programmes whose lines expire `EB_RA_LEAD_DAYS` ahead, sends the renewal advice stored as `RENEWAL_ADVICE`, and the reminders at `EB_RA_REMINDER_DAYS` while no feedback is recorded). Job built by wave E1-C; cron configured since E0. Property `brokerverse.jobs.eb-renewal-advice-cron`. |
| `BROKERVERSE_JOB_EB_ITEM_FOLLOWUP_CRON` | no | `0 0 23 * * *` | Spring cron (UTC) of `EB_ITEM_FOLLOWUP` (eb, BRID-030; 07:00 PHT: follow-up e-mails for tracked items past due every `EB_FOLLOWUP_DAYS`, escalation `EB_ITEM_ESCALATED` after `EB_FOLLOWUP_MAX`). Job built by wave E1-C; cron configured since E0. Property `brokerverse.jobs.eb-item-followup-cron`. |
| `BROKERVERSE_JOBS_USER_SESSION_SWEEP_CRON` | no | `0 */15 * * * *` | Spring cron (UTC) of `USER_SESSION_SWEEP` (security, UAM-NFR-35): ends the sign-in sessions nobody signed out of - idle longer than `SESSION_TIMEOUT_MINUTES` plus 5 minutes (IDLE_TIMEOUT), token expired (EXPIRED), user locked (LOCKED) or disabled (ADMIN_ENDED). Property `brokerverse.jobs.user-session-sweep-cron` (default in code, not in `application.yml`). |
| `BROKERVERSE_SECURITY_PASSWORD_RESET_URL` | no | first allowed origin + `/reset-password` | Address of the web page opened by the "Forgot password?" e-mail link (UAM-NFR-37; the link carries a single-use token valid 30 minutes). Property `brokerverse.security.password-reset-url`; set it when the web client is not served from the first CORS origin. |
| `BROKERVERSE_JOB_EVENT_OUTBOX_RELAY_CRON` | no | `0 * * * * *` | Spring cron (UTC) of `EVENT_OUTBOX_RELAY` (platform, every minute): sends the integration events the after-commit relay left in `evt_outbox` (broker down, instance stopped) and the retries that are due; with Kafka disabled marks leftovers `LOCAL`. Property `brokerverse.jobs.event-outbox-relay-cron`. |
| `BROKERVERSE_JOB_EVENT_HOUSEKEEPING_CRON` | no | `0 50 0 * * *` | Spring cron (UTC) of `EVENT_HOUSEKEEPING` (platform, daily): deletes delivered outbox rows older than `BROKERVERSE_KAFKA_OUTBOX_RETENTION`, archived events and resolved dead letters older than `BROKERVERSE_KAFKA_ARCHIVE_RETENTION`. Property `brokerverse.jobs.event-housekeeping-cron`. |
| `BROKERVERSE_JOB_SHARED_STATE_CLEANUP_CRON` | no | `0 40 0 * * *` | Spring cron (UTC) of `SHARED_STATE_CLEANUP` (platform, daily): deletes expired rows of `sec_revoked_token` and `sys_shared_counter` (database fallback of Redis). Property `brokerverse.jobs.shared-state-cleanup-cron`. |
| `BROKERVERSE_JOB_LOCK_LEASE` | no | `PT2M` | Lease of the Redis job lock, renewed every third of it while the job runs; an instance that dies frees the lock after at most one lease. Property `brokerverse.jobs.lock-lease`. |
| `BROKERVERSE_MAIL_ENABLED` | no | `false` | `true` delivers e-mails through the SMTP server below; `false` records them in the outbox as *simulated* (seed, test, UAT without mail). Addresses ending in `.invalid` are always rejected by the simulated transport. |
| `BROKERVERSE_MAIL_DISPATCH_ON_COMMIT` | no | `true` | Deliver right after the business transaction commits; `false` leaves delivery to the `MAIL_DISPATCH` job only. |
| `MAIL_HOST` / `MAIL_PORT` | when mail enabled | `localhost` (none with `prod`) / `587` | SMTP server. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | when the server requires it (always in production with mail enabled and `MAIL_SMTP_AUTH`) | — | SMTP credentials (secret: supply from the vault, never in files). |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | no | `true` / `true` | SMTP authentication and STARTTLS. |
| `BROKERVERSE_BACKEND_HOST` (frontend container) | yes | `backend` | Host name of the backend service for the `/api` proxy. |

## Redis 7 and Apache Kafka (platform cache and events)

Design, topic catalogue and runbook: [`PLATFORM_CACHE_AND_EVENTS.md`](../architecture/PLATFORM_CACHE_AND_EVENTS.md).
Both default to **on** in `application.yml` and are **off** in the `test` profile. With either off the
application starts and behaves correctly on its fallbacks (in-memory cache, PostgreSQL advisory locks
and tables of V28, events recorded `LOCAL`, e-mail sent after commit). Every instance of one
environment must use the same settings.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `BROKERVERSE_REDIS_ENABLED` | yes (`true`) | `true` | `brokerverse.redis.enabled`. `true`: Redis holds the reference-data cache, the job locks, the token denylist and the shared counters; `false`: in-memory cache (per instance, bounded by the time to live), PostgreSQL advisory job locks, tables `sec_revoked_token` / `sys_shared_counter`. Also switches the Redis health check (`management.health.redis.enabled`). |
| `BROKERVERSE_REDIS_HOST` / `BROKERVERSE_REDIS_PORT` | when enabled | `localhost` / `6379` | `spring.data.redis.host` / `port`. On AWS: the ElastiCache (Redis 7) primary endpoint. |
| `BROKERVERSE_REDIS_USERNAME` | no | – | `spring.data.redis.username` (ElastiCache RBAC user; blank = default user). |
| `BROKERVERSE_REDIS_PASSWORD` | when the server requires it; always in production with Redis enabled | – | `spring.data.redis.password` (ElastiCache AUTH token or RBAC password). **Secret**: from the vault. |
| `BROKERVERSE_REDIS_TLS` | yes on AWS | `false` | `spring.data.redis.ssl.enabled`: `true` with ElastiCache in-transit encryption. |
| `BROKERVERSE_REDIS_DATABASE` | no | `0` | `spring.data.redis.database`. |
| `BROKERVERSE_REDIS_TIMEOUT` / `BROKERVERSE_REDIS_CONNECT_TIMEOUT` | no | `2s` / `5s` | Command and connect time-outs. |
| `BROKERVERSE_REDIS_KEY_PREFIX` | no | `bv:` | `brokerverse.redis.key-prefix`: prefix of every key (`bv:cache:…`, `bv:joblock:…`, `bv:session:revoked:…`, `bv:counter:…`); use one per environment when environments share a Redis. |
| `BROKERVERSE_CACHE_TTL_LOV` | no | `PT1H` | `brokerverse.cache.ttl.lov-values`: time to live of the list-of-values cache. |
| `BROKERVERSE_CACHE_TTL_PARAMETERS` | no | `PT15M` | `brokerverse.cache.ttl.system-parameters`. |
| `BROKERVERSE_CACHE_TTL_ROLE_PERMISSIONS` | no | `PT15M` | `brokerverse.cache.ttl.security-role-permissions`. |
| `BROKERVERSE_CACHE_TTL_CATALOG` | no | `PT1H` | `brokerverse.cache.ttl.catalog-product-versions`. |
| `BROKERVERSE_CACHE_TTL_ORGANIZATION` | no | `PT1H` | `brokerverse.cache.ttl.organization-units`. |
| `BROKERVERSE_CACHE_MAX_SIZE` | no | `10000` | `brokerverse.cache.maximum-size`: entries per cache of the in-memory fallback. |
| `BROKERVERSE_LOGIN_RATE_LIMIT` | no | `20` | `brokerverse.security.login-protection.max-attempts-per-window`: login requests accepted per client address and window, counted across all instances; above it `POST /auth/login` answers HTTP 429 (`LOGIN_RATE_LIMITED`). Behind the ingress set `SERVER_FORWARD_HEADERS_STRATEGY=native` (or `framework`) so the address is the caller's. |
| `BROKERVERSE_LOGIN_RATE_WINDOW` | no | `PT1M` | `brokerverse.security.login-protection.rate-limit-window`. |
| `BROKERVERSE_LOGIN_FAILURE_WINDOW` | no | `P1D` | `brokerverse.security.login-protection.failed-attempt-window`: life of the shared failed-login counter of a user. The lockout itself still follows `LOGIN_MAX_FAILED_ATTEMPTS`; `sec_user.failed_attempts` stays the record. |
| `BROKERVERSE_KAFKA_ENABLED` | yes (`true`) | `true` | `brokerverse.kafka.enabled`. `true`: the outbox is relayed to Kafka and the consumers run (archive, e-mail dispatch, dead-letter recorder); `false`: events are recorded as delivered in-process (`LOCAL`) and e-mails are sent after commit and by `MAIL_DISPATCH`. |
| `BROKERVERSE_KAFKA_BOOTSTRAP_SERVERS` | when enabled | `localhost:9092` | `spring.kafka.bootstrap-servers`. On AWS: the Amazon MSK bootstrap brokers (SASL/SCRAM port 9096 or TLS port 9094). |
| `BROKERVERSE_KAFKA_SECURITY_PROTOCOL` | yes on AWS | `PLAINTEXT` | `spring.kafka.properties.security.protocol`: `SASL_SSL` on MSK with SASL/SCRAM, `SSL` with TLS only. |
| `BROKERVERSE_KAFKA_SASL_MECHANISM` | with SASL | `SCRAM-SHA-512` | `spring.kafka.properties.sasl.mechanism`. |
| `BROKERVERSE_KAFKA_SASL_JAAS_CONFIG` | with SASL; always in production with Kafka enabled | – | `spring.kafka.properties.sasl.jaas.config`, e.g. `org.apache.kafka.common.security.scram.ScramLoginModule required username="…" password="…";` (MSK secret in AWS Secrets Manager). **Secret**. |
| `BROKERVERSE_KAFKA_CLIENT_ID` | no | `brokerverse` | `spring.kafka.client-id`. |
| `BROKERVERSE_KAFKA_CREATE_TOPICS` | no | `true` | `spring.kafka.admin.auto-create`: the application creates missing topics (and their `.dlt` topics) at start-up with the partitions and replication factor below. Broker-side auto-creation is off (`allow.auto.create.topics=false` on every client; `auto.create.topics.enable=false` on MSK). Set `false` when the topics are provisioned by infrastructure code. |
| `BROKERVERSE_KAFKA_PARTITIONS` | no | `3` | `brokerverse.kafka.partitions` of each topic created. |
| `BROKERVERSE_KAFKA_REPLICATION_FACTOR` | yes on MSK | `1` | `brokerverse.kafka.replication-factor`: `3` on a three-AZ MSK cluster. |
| `BROKERVERSE_KAFKA_GROUP_PREFIX` | no | `bibs` | `brokerverse.kafka.consumer-group-prefix` of the consumer groups (`bibs-event-archive`, `bibs-mail-dispatch`, `bibs-dead-letter-recorder`). |
| `BROKERVERSE_KAFKA_RELAY_BATCH_SIZE` | no | `200` | Outbox rows sent per relay round. |
| `BROKERVERSE_KAFKA_RELAY_MAX_ATTEMPTS` | no | `10` | Send attempts of an outbox row before it becomes `FAILED` (support screen: *Send Again*). |
| `BROKERVERSE_KAFKA_RELAY_RETRY_BACKOFF` | no | `PT30S` | First retry delay of a row; doubles per attempt, at most one hour. |
| `BROKERVERSE_KAFKA_SEND_TIMEOUT` | no | `PT30S` | Wait for the broker acknowledgement of a send. |
| `BROKERVERSE_KAFKA_DELIVERY_TIMEOUT_MS` / `BROKERVERSE_KAFKA_MAX_BLOCK_MS` | no | `30000` / `15000` | Producer `delivery.timeout.ms` / `max.block.ms`. |
| `BROKERVERSE_KAFKA_CONSUMER_RETRIES` | no | `3` | Retries of a failing consumed record before it goes to `<topic>.dlt`. |
| `BROKERVERSE_KAFKA_CONSUMER_RETRY_BACKOFF` | no | `PT2S` | Delay between consumer retries. |
| `BROKERVERSE_KAFKA_OUTBOX_RETENTION` | no | `P30D` | Age after which delivered outbox rows (`SENT`, `LOCAL`) are deleted. |
| `BROKERVERSE_KAFKA_ARCHIVE_RETENTION` | no | `P400D` | Age after which archived events and resolved dead letters are deleted. |

Fixed producer / consumer settings (`application.yml`): `acks=all`, `enable.idempotence=true`,
`max.in.flight.requests.per.connection=5` (order per partition kept), consumer
`auto-offset-reset=earliest`, `enable-auto-commit=false`, `isolation.level=read_committed`.

Every background job is a `ManagedJob` listed on *Administration › Scheduled Jobs* with its next
run, run history and "Run now". A cron of `-` disables the schedule (manual runs only); cron
expressions have six fields (second minute hour day month weekday) and are evaluated in UTC, and
the business date of a scheduled run is the UTC date.

Dashboard KPI mapping (optional, `application.yml` or env `BROKERVERSE_DASHBOARD_CASH_GROUPS_0` …):
`brokerverse.dashboard.cash-groups`, `receivable-groups`, `reserve-groups` list chart-of-accounts
statement lines (`report_group`) used for the cash, receivables and technical reserve tiles.

## Production start-up safeguards

`ProductionSafeguards` (package `config`, registered in `META-INF/spring.factories`) checks the resolved
configuration before any bean is created. A production start is one with the `prod` profile or with
`BROKERVERSE_ENVIRONMENT=production`; the `prod` profile sets `production` itself. BIBS refuses to start, and lists
every problem in one message, when:

- the `seed` profile is active in production (seed data never loads in production);
- `BROKERVERSE_DB_URL`, `BROKERVERSE_DB_USER` or `BROKERVERSE_DB_PASSWORD` is missing (the `prod` profile has no
  defaults for them);
- `BROKERVERSE_JWT_SECRET` is missing, shorter than 32 characters or a development value;
- mail delivery is on (`BROKERVERSE_MAIL_ENABLED=true`) and `MAIL_HOST`, or with SMTP authentication
  `MAIL_USERNAME` / `MAIL_PASSWORD`, is missing;
- Redis is on (`BROKERVERSE_REDIS_ENABLED`, default `true`) and `BROKERVERSE_REDIS_PASSWORD` is missing;
- Kafka is on (`BROKERVERSE_KAFKA_ENABLED`, default `true`) and the protocol is not SASL or
  `BROKERVERSE_KAFKA_SASL_JAAS_CONFIG` is missing.

The base `application.yml` holds no password or signing key. Only the `seed` profile (local stacks, SIT, UAT and
training) and the automated tests carry local values, and the `seed` profile is refused in production.

## Seed data (SIT, UAT and training)

The `seed` profile adds the Flyway location `classpath:db/seed` (versions V900-V999 and V1900-V1999) and the seed
start-up runners (`*.seed` packages, `*SeedData` classes). It creates the seed company FVI under the client's legal
name, BDO Insurance and Reinsurance Brokers, Inc., its branches, chart of accounts, seed records and the SIT/UAT
users. Seed record numbers are plain sequence numbers in the 9000xx range (for example `CL-2026-900001`,
`AR-2026-900001`). The SIT/UAT password is held in the seed configuration and issued by the project team; it is not
written in the client documents.

- **Not yet applied anywhere.** The seed migrations were renamed to `db/seed/V9xx__seed_*.sql` and
  `V19xx__seed_*.sql` (with their record codes and names) before any environment applied them, so no Flyway history
  refers to the former names. A database created from an earlier local build is dropped and recreated; `flyway
  repair` is not needed on SIT, UAT or production.
- **Comment-only edits of schema migrations.** The same change reworded comments of some `db/migration` files
  (V652, V764, V771, V870, V880, V890, V1000, V1020, V1050-V1052, V1055, V1060). No SQL statement changed, but the Flyway
  checksums did: a database migrated by an earlier build runs `flyway repair` once (or is recreated) before the
  next start.
