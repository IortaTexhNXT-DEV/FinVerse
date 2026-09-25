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
| `BROKERVERSE_JOB_PACKAGE_EXPIRY_CRON` | no | `0 0 17 * * *` | Spring cron (UTC) of `PACKAGE_EXPIRY_MONITOR` (Product Maintenance, BRPM.006/017), daily 01:00 PHT: moves package versions to SUPERSEDED / EXPIRED, raises `PACKAGE_EXPIRING` at `PACKAGE_EXPIRY_NOTICE_DAYS` and at 30 / 7 days, and drafts RENEW package requests when `PACKAGE_RENEWAL_AUTODRAFT` is true. Note that the business date of the 17:00 UTC run is the PHT date of the previous day. Property `brokerverse.jobs.package-expiry-cron`. |
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
