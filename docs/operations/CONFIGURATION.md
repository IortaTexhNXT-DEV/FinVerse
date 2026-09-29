# Configuration Reference

All settings are environment variables (12-factor). Defaults in `backend/src/main/resources/application.yml`
are for local development only.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | – | `prod` in production. `seed` loads the seed data of SIT, UAT and training (never in production: the start is refused, see "Start-up safeguards"). |
| `BROKERVERSE_ENVIRONMENT` | yes | `local` | `brokerverse.environment`: `local`, `sit`, `uat`, `training`, `preprod` or `production`. Every value except `local` turns on the secret checks of the start-up safeguards; `production` (set by the `prod` profile) adds the transport checks. |
| `BROKERVERSE_BUSINESS_ZONE` | no | `Asia/Manila` | `brokerverse.business-zone`: time zone of the business date (`BusinessClock`). "Today", "not in the future" and cut-off checks, accounting periods, report date keywords, job run dates and the report day cuts use the calendar day of this zone; timestamps stay UTC and the job cron expressions are UTC. An unknown zone refuses the start. Change it only with a new deployment, never on a running platform. |
| `BROKERVERSE_DB_URL` | yes | `jdbc:postgresql://localhost:5432/brokerverse`; none with `prod` | JDBC URL. Leave `sslmode` out: `BROKERVERSE_DB_SSL_MODE` applies (an `sslmode` in the URL takes precedence, and a plaintext one is refused in production). |
| `BROKERVERSE_DB_USER` | yes | `brokerverse`; none with `prod` | Database login of the application. In production a least-privilege runtime login, member of the runtime role (`BROKERVERSE_DB_RUNTIME_ROLE`), never the schema owner; in development and tests it may be the owner (then Flyway migrates with it). See DEPLOYMENT.md "Database roles". |
| `BROKERVERSE_DB_PASSWORD` | yes | none (a local value with the `seed` profile only) | Database password (from secret store). |
| `SPRING_FLYWAY_USER` | yes | – (the application login) | `spring.flyway.user`: owner of the schema objects; Flyway migrates on start with this login. Must differ from `BROKERVERSE_DB_USER` in production. The migration connection uses the same URL and TLS settings (`BROKERVERSE_DB_SSL_MODE`, `BROKERVERSE_DB_SSL_ROOT_CERT`) as the application pool (`MigrationOwnerConnection`). |
| `SPRING_FLYWAY_PASSWORD` | yes | – | `spring.flyway.password`: password of the schema owner (from secret store). |
| `BROKERVERSE_DB_RUNTIME_ROLE` | no | `brokerverse_runtime` | Flyway placeholder `runtime_role`: the group role that migration V1190 grants row access to (SELECT, INSERT, UPDATE, DELETE, sequences; no DDL, no TRUNCATE, no triggers, read-only migration history), also for the tables of later migrations (default privileges). V1190 creates it (`NOLOGIN`) when the schema owner may create roles; otherwise the DBA creates it first. Set it before the first start; changing it later needs the grants repeated for the new role. |
| `BROKERVERSE_DB_POOL_SIZE` | no | `20` | Hikari maximum pool size per instance. |
| `BROKERVERSE_JWT_SECRET` | yes (every environment but local) | none (the automated tests and the local docker compose stack carry local values; the `seed` profile has none) | HMAC key for access tokens, ≥ 32 random characters. Rotating it makes every access token invalid; the refresh cookies renew them. |
| `BROKERVERSE_TOKEN_VALIDITY` | no | `PT8H` | Maximum life of a sign-in session (ISO-8601 duration): the access token is renewed until then. |
| `BROKERVERSE_ACCESS_TOKEN_VALIDITY` | no | `PT15M` | Life of an access token when the security parameter `ACCESS_TOKEN_MINUTES` (15, between 5 and 60) is missing. |
| `BROKERVERSE_REFRESH_GRACE` | no | `PT30S` | Time a replaced refresh token is still accepted (a second browser tab renewing at the same moment); later it ends the session (`TOKEN_REUSED`). |
| `BROKERVERSE_ALLOWED_ORIGINS` | yes (every environment but local, not localhost) | `http://localhost:5173` | Comma separated browser origins allowed by CORS. |
| `BROKERVERSE_MAX_FAILED_ATTEMPTS` | no | `5` | Consecutive failed sign-ins (wrong password or wrong second-factor code) that lock an account when the business parameter `LOGIN_MAX_FAILED_ATTEMPTS` is missing. The parameter wins (the client's value is set in its data; a security parameter, changed under a second approval on *Administration › System Parameters*). Property `brokerverse.security.max-failed-attempts`. |
| `BROKERVERSE_ADMIN_USERNAME` | first start | `sysadmin` | Initial administrator (created only when no user exists). |
| `BROKERVERSE_ADMIN_INITIAL_PASSWORD` | first start | – | Initial administrator password: a one-time value that must be changed at the first sign-in (it ages like any other password); remove it from the secret afterwards. |
| `BROKERVERSE_SEED_PASSWORD` | no (seed profile only) | – | Password of the SIT/UAT users of this seed environment (SIT, UAT, training, local stacks), from the secret store; never written into a file. At start the seed profile gives it to every SIT/UAT user still carrying the password hash of the seed scripts (`SeedPasswords`). Blank: the users are left unchanged and a warning is logged. See "Seed data". |
| `BROKERVERSE_SEED_PASSWORD_MUST_CHANGE` | no | `false` | `true`: the SIT/UAT users given `BROKERVERSE_SEED_PASSWORD` must change it at their first sign-in (each tester then holds an own password; shared personas and the screenshot tools need the unchanged password, hence the default). |
| `BROKERVERSE_PORT` | no | `8080` | Listener port (`8443` in the Kubernetes deployments, where it serves HTTPS). |
| `BROKERVERSE_JOB_RECURRING_CRON` | no | `0 0 1 * * *` | Spring cron (UTC) of `RECURRING_JOURNALS`: generates the due recurring and accrual journals. |
| `BROKERVERSE_JOB_ALERTS_CRON` | no | `0 30 1 * * *` | Spring cron (UTC) of `ALERT_DAILY_CHECKS`: evaluates the scheduled exception codes. |
| `BROKERVERSE_JOB_RESERVE_VALUATION_CRON` | no | `-` (off) | Spring cron (UTC) of the monthly actuarial reserve valuation of the previous month. |
| `BROKERVERSE_JOB_RI_ALLOCATION_CRON` | no | `-` (off) | Spring cron (UTC) of the scheduled reinsurance allocation run; the run can always be started on demand. |
| `BROKERVERSE_JOB_PDC_ISSUED_DUE_CRON` | no | `0 15 0 * * *` | Spring cron (UTC) of `PDC_ISSUED_DUE`: post-dated cheques issued whose cheque date is reached become DUE (status only, no posting). Replaces the former `brokerverse.payables.pdc-due-cron`. |
| `BROKERVERSE_JOB_QUOTATION_EXPIRY_CRON` | no | `-` (off) | Spring cron (UTC) of `QUOTATION_EXPIRY`: open insurer underwriting quotations of every active company past their validity become EXPIRED. Insurer suite, not used by BIBS, so off by default since V1064 (`CODEBASE_RELEVANCE_AUDIT.md` A1); the former default was `0 45 0 * * *`. |
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
| `BROKERVERSE_JOB_UAM_DORMANT_USERS_CRON` | no | `0 15 16 * * *` | Spring cron (UTC) of `UAM_DORMANT_USERS` (nbadmin, V1065; 00:15 PHT: deactivates the users without a sign-in for `UAM_DORMANT_DAYS` days by a system Deactivate user request, tells them `UAM_DORMANT_NOTICE_DAYS` days before; `UAM_DORMANT_DAYS` = 0 switches it off; the holders of SYSADMIN are exempt). Property `brokerverse.jobs.uam-dormant-users-cron`. |
| `BROKERVERSE_JOB_PASSWORD_EXPIRY_NOTICE_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `PASSWORD_EXPIRY_NOTICE` (nbadmin, UAM-NFR-36; 06:00 PHT, `AUTH_MODE` LOCAL only: notifies the users whose password expires within 7 days of `PASSWORD_MAX_AGE_DAYS`, in the app and by e-mail, event `PASSWORD_EXPIRY_NOTICE`). Property `brokerverse.jobs.password-expiry-notice-cron`. |
| `BROKERVERSE_JOB_BCL_PREMIUM_RECHECK_CRON` | no | `0 30 21 * * *` | Spring cron (UTC) of `BCL_PREMIUM_RECHECK` (brokerclaims, BRCLM.001; 05:30 PHT: re-runs the premium check of open claims without an authorization code against the invoice ledger, as a safety net for missed `InvoiceMovementPosted` events; raises or resolves `BCL_UNPAID_PREMIUM_CLAIM`). Property `brokerverse.jobs.bcl-premium-recheck-cron`. |
| `BROKERVERSE_JOB_BCL_FOLLOW_UP_DUE_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `BCL_FOLLOW_UP_DUE` (brokerclaims, BRCLM.019/022/034; 06:00 PHT: notifies the handlers of claims whose next follow-up date or diary due date is today (`BCL_FOLLOW_UP_DUE`) and raises `BCL_FOLLOW_UP_OVERDUE` for past dates). Property `brokerverse.jobs.bcl-follow-up-due-cron`. |
| `BROKERVERSE_JOB_BCL_AGEING_ALERTS_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `BCL_AGEING_ALERTS` (brokerclaims, BRCLM.031, p.43; 06:00 PHT: raises `BCL_CLAIM_PAST_DUE` for outstanding claims older than `BCL_PAST_DUE_DAYS`). Property `brokerverse.jobs.bcl-ageing-alerts-cron`. |
| `BROKERVERSE_JOB_EB_RENEWAL_ADVICE_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `EB_RENEWAL_ADVICE` (eb, BRID-001/002; 06:00 PHT: opens the RENEWAL cycles of the ACTIVE programmes flagged for renewal whose lines expire within `EB_RA_LEAD_DAYS`, sends the password-protected renewal advice (password in a second e-mail) to the HR contacts that receive it, stores it as `RENEWAL_ADVICE` linked to the programme, the client and the expiring accounts, raises `EB_RA_NOT_SENT` for programmes that cannot receive it, then sends one reminder per run for each `EB_RA_REMINDER_DAYS` day reached while no feedback is recorded). Each programme and reminder runs in its own transaction. Built by wave E1-C (`eb.renewal.service.RenewalAdviceJob`). Property `brokerverse.jobs.eb-renewal-advice-cron`. |
| `BROKERVERSE_JOB_EB_ITEM_FOLLOWUP_CRON` | no | `0 0 23 * * *` | Spring cron (UTC) of `EB_ITEM_FOLLOWUP` (eb, BRID-030; 07:00 PHT: for each pending tracked item past due, the n-th follow-up e-mail (template `EB_ITEM_FOLLOWUP`) `n x EB_FOLLOWUP_DAYS` working days (head office calendar) after the due date to the item's recipients, else the insurer's placement mailboxes, the client's HR contacts or the AO; after `EB_FOLLOWUP_MAX` follow-ups the alert `EB_ITEM_OVERDUE` and the notice `EB_ITEM_ESCALATED` to the AO, once per item). Built by wave E1-C (`eb.tracked.service.ItemFollowUpJob`). Property `brokerverse.jobs.eb-item-followup-cron`. |
| `BROKERVERSE_JOB_EB_FRANCHISE_EXPIRY_CRON` | no | `0 30 22 * * *` | Spring cron (UTC) of `EB_FRANCHISE_EXPIRY` (eb, franchise; 06:30 PHT: a franchise request still without the insurer's decision `EB_FRANCHISE_GRACE_DAYS` working days (head office calendar) after its due date is expired and the AO is told; the daily alert check raises `EB_FRANCHISE_OVERDUE` from the due date on). Each request runs in its own transaction. Built by wave E1-B (`eb.franchise.service.FranchiseExpiryJob`). Property `brokerverse.jobs.eb-franchise-expiry-cron`. |
| `BROKERVERSE_JOB_MIG_INTAKE_SCAN_CRON` | no | `-` (manual) | Spring cron (UTC) of `MIG_INTAKE_SCAN` (Data Migration BRD-13): takes the extracts waiting in the extract inbox through the intake checks. The default inbox is the console upload, which has nothing to collect; the SFTP drop adapter is parked (DATA_MIGRATION_DESIGN section 23). Property `brokerverse.jobs.mig-intake-scan-cron`. |
| `BROKERVERSE_JOB_MIG_VALIDATE_CRON`, `BROKERVERSE_JOB_MIG_LOAD_CRON`, `BROKERVERSE_JOB_MIG_RECONCILE_CRON` | no | `-` (manual) | Spring crons (UTC) of `MIG_VALIDATE`, `MIG_LOAD` and `MIG_RECONCILE`: validation, load and reconciliation of the planned batches. They run on demand from the migration console; a schedule is set only for unattended mock runs. Properties `brokerverse.jobs.mig-validate-cron`, `mig-load-cron`, `mig-reconcile-cron`. |
| `BROKERVERSE_MIGRATION_MASKING_KEY` | yes, outside production | – (seed profile: a local key) | `brokerverse.migration.masking-key`: key of the keyed masking (HMAC-SHA256) of personal data in non-production extracts, from the secrets store. While it is empty the intake refuses extracts outside production; never reuse the key between environments. |
| `BROKERVERSE_JOB_MIG_STAGING_PURGE_CRON` | no | `0 0 18 * * *` | Spring cron (UTC) of `MIG_STAGING_PURGE` (02:00 PHT): purges the staged payloads and intake files of the batches past `MIG_STAGING_RETENTION_DAYS`; the counts and totals are kept. Property `brokerverse.jobs.mig-staging-purge-cron`. |
| `BROKERVERSE_JOB_MIG_RUNOFF_SNAPSHOT_CRON` | no | `0 30 17 1 * *` | Spring cron (UTC) of `MIG_RUNOFF_SNAPSHOT` (01:30 PHT on the 1st): counts the legacy in-force policy headers by expiry month and source system - renewed, not renewed, lapsed, still open - for the Run-off screen and report `MIG-RUNOFF`. Property `brokerverse.jobs.mig-runoff-snapshot-cron`. |
| `BROKERVERSE_JOB_MIG_ACCESS_LOG_DIGEST_CRON` | no | `0 0 0 1 * *` | Spring cron (UTC) of `MIG_ACCESS_LOG_DIGEST` (08:00 PHT on the 1st): sends the holders of `LEGACY_ACCESS_LOG_VIEW` the digest of the previous month's accesses to the legacy archive per user and action. Property `brokerverse.jobs.mig-access-log-digest-cron`. |
| `BROKERVERSE_JOB_RENEWAL_EXTRACTION_CRON` | no | `0 0 17 * * *` | Spring cron (UTC) of `RNW_EXTRACTION` (renewal, BRRN.030; 01:00 PHT: extracts the accounts and migrated policies expiring at the business date plus `RNW_EXTRACTION_LEAD_DAYS`, once per term, and runs their checks; raises `RNW_EXTRACTION_FAILED` on failure). Built by wave R1-A (`renewal.extraction.service.ExtractionJob`). Property `brokerverse.jobs.renewal-extraction-cron`. |
| `BROKERVERSE_JOB_RENEWAL_REEVALUATE_CRON` | no | `0 30 17 * * *` | Spring cron (UTC) of `RNW_REEVALUATE` (renewal, BRRN.023; 01:30 PHT: runs the checks again for the open renewals changed since the last run or expiring within 30 days). Built by wave R1-A (`renewal.rules.service.ReevaluateJob`). Property `brokerverse.jobs.renewal-reevaluate-cron`. |
| `BROKERVERSE_JOB_RENEWAL_NRNS_LETTERS_CRON` | no | `0 0 22 * * *` | Spring cron (UTC) of `RNW_NRNS_LETTERS` (renewal, BRRN.025/037; 06:00 PHT: flags the renewals with no response after the Renewal Advice, sends the NRNS reminder and offers the second notice). Built by wave R1-D (`renewal.letter.service.NrnsLettersJob`). Property `brokerverse.jobs.renewal-nrns-letters-cron`. |
| `BROKERVERSE_JOB_RENEWAL_EXPIRY_SWEEP_CRON` | no | `0 15 16 * * *` | Spring cron (UTC) of `RNW_EXPIRY_SWEEP` (renewal, BRRN.037; 00:15 PHT: closes the renewals still waiting for the client past expiry plus `RNW_NON_ACCEPTANCE_DAYS` with the non-acceptance letter). Built by wave R1-D (`renewal.letter.service.ExpirySweepJob`). Property `brokerverse.jobs.renewal-expiry-sweep-cron`. |
| `BROKERVERSE_JOB_RENEWAL_LETTER_BATCH_CRON` | no | `0 5 * * * *` | Spring cron (UTC) of `RNW_LETTER_BATCH` (renewal; hourly at minute 5: reads the delivery status of the renewal letters sent by e-mail and raises `RNW_LETTER_FAILED` for the failed ones). Built by wave R1-D (`renewal.letter.service.LetterBatchJob`). Property `brokerverse.jobs.renewal-letter-batch-cron`. |
| `BROKERVERSE_JOB_RENEWAL_GOLIVE_EXTRACTION_CRON` | no | `-` (not scheduled) | Spring cron (UTC) of `RNW_GOLIVE_EXTRACTION` (renewal, DMQ37: go-live take-over of the migrated policies expiring from go-live to `MIG_GOLIVE_RENEWAL_TO`, urgent up to `MIG_RENEWAL_URGENT_TO`). Run once from the cut-over runbook (Renewal Setup, Go-live, or a one-off cron). Built by wave R1-A (`renewal.extraction.service.GoLiveExtractionJob`). Property `brokerverse.jobs.renewal-golive-extraction-cron`. |
| `BROKERVERSE_JOB_SBM_INTAKE_PULL_CRON` | no | `-` (not scheduled) | Spring cron (UTC) of `SBM_INTAKE_PULL` (submitted, BRIDSP-01/13: pulls the files of every active source through the `SubmittedSourceFeed` port and loads each like an upload of its source). Off until a bank transport replaces the default adapter, which delivers nothing. Built by Submitted Policies (`submitted.intake.service.IntakePullJob`). Property `brokerverse.jobs.sbm-intake-pull-cron`. |
| `BROKERVERSE_JOB_SBM_PROCESSING_CRON` | no | `0 30 13 * * *` | Spring cron (UTC) of `SBM_PROCESSING` (submitted, BRIDSP-09; 21:30 PHT: for every company one processing run of the records received, validated, classified, in review or waiting for a manual disposition, 200 records per transaction). Built by Submitted Policies (`submitted.processing.service.ProcessingJob`). Property `brokerverse.jobs.sbm-processing-cron`. |
| `BROKERVERSE_JOB_SBM_EXPIRY_SCAN_CRON` | no | `0 0 14 * * *` | Spring cron (UTC) of `SBM_EXPIRY_SCAN` (submitted, BRIDSP-23; 22:00 PHT: hands the records For Renewal whose expiry is within `SBM_RENEWAL_LEAD_DAYS` of their segment to Renewal with the insurer of the insurer rules, and offers again the hand-offs Renewal has not taken). One transaction per company. Built by Submitted Policies (`submitted.renewal.service.ExpiryScanJob`). Property `brokerverse.jobs.sbm-expiry-scan-cron`. |
| `BROKERVERSE_JOB_SBM_LETTER_DISPATCH_CRON` | no | `0 30 22 * * *` | Spring cron (UTC) of `SBM_LETTER_DISPATCH` (submitted, BRIDSP-22/24; 06:30 PHT: writes and sends the letters due by the letter rules, by e-mail or to the bank counterpart, and hands the printed ones to the mail house as print batches). Built by Submitted Policies (`submitted.renewal.service.LetterDispatchJob`). Property `brokerverse.jobs.sbm-letter-dispatch-cron`. |
| `BROKERVERSE_JOB_SBM_HOLD_COVER_WATCH_CRON` | no | `0 0 23 * * *` | Spring cron (UTC) of `SBM_HOLD_COVER_WATCH` (submitted, BRIDSP-32; 07:00 PHT: raises `SBM_INSURER_NOT_ACCEPTED` for hold covers not accepted within `SBM_INSURER_ACCEPT_DAYS`, `SBM_HOLD_COVER_UNBOOKED` for confirmed hold covers of unbooked renewals ending within `SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS`, and closes the records whose renewal Renewal closed without booking). Built by Submitted Policies (`submitted.renewal.service.HoldCoverWatchJob`). Property `brokerverse.jobs.sbm-hold-cover-watch-cron`. |
| `BROKERVERSE_JOB_SBM_HANDLING_FEE_TAGGER_CRON` | no | `0 */30 * * * *` | Spring cron (UTC) of `SBM_HANDLING_FEE_TAGGER` (submitted, BRIDSP-31; every 30 minutes: tags the open unapplied payments to the billed handling fees by PN (CLPC) or location (over the counter) and asks Cashiering to recognise them as income with an official receipt; payments matching several fees wait for the handler). Built by Submitted Policies (`submitted.fee.service.HandlingFeeTaggerJob`). Property `brokerverse.jobs.sbm-handling-fee-tagger-cron`. |
| `BROKERVERSE_JOB_CSF_LEGACY_SYNC_CRON` | no | `-` (manual) | Spring cron (UTC) of `CSF_LEGACY_SYNC` (Customer Servicing Facility, FR-CSF-022; sends the contact changes of the contact centre to QPS and EBIX through the port `ContactSyncGateway`, replaying the changes kept while the sync was off). Manual until the legacy interface exists and the parameter `CSF_LEGACY_SYNC_ENABLED` is on; then `0 */15 * * * *` (every 15 minutes). Workload INTEGRATION (runs on `bibs-integration`). Built by the Customer Servicing Facility (`csf.service.LegacySyncJob`). Property `brokerverse.jobs.csf-legacy-sync-cron`. |
| `BROKERVERSE_JOBS_USER_SESSION_SWEEP_CRON` | no | `0 */15 * * * *` | Spring cron (UTC) of `USER_SESSION_SWEEP` (security, UAM-NFR-35): ends the sign-in sessions nobody signed out of - idle longer than `SESSION_TIMEOUT_MINUTES` plus 5 minutes (IDLE_TIMEOUT), token expired (EXPIRED), user locked (LOCKED) or disabled (ADMIN_ENDED). Property `brokerverse.jobs.user-session-sweep-cron` (default in code, not in `application.yml`). |
| `BROKERVERSE_PASSWORD_RESET_URL` | yes (every environment but local, not localhost) | on a developer's machine the first allowed origin + `/reset-password` | Address of the web page opened by the "Forgot password?" e-mail link (UAM-NFR-37; the link carries a single-use token valid 30 minutes), e.g. `https://bibs.example.com/reset-password`. Property `brokerverse.security.password-reset-url` (the relaxed name `BROKERVERSE_SECURITY_PASSWORD_RESET_URL` works too). |
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

## Data Migration parameters (BRD-13)

Business parameters (`sys_parameter`, category `DATA_MIGRATION`, maintained in Administration > Parameters) of the
migration console, the legacy context and the Legacy Inquiry ([`DATA_MIGRATION_DESIGN.md`](../architecture/DATA_MIGRATION_DESIGN.md)
section 20). The seed data sets the opening value date inside the open seed year; production keeps the defaults below
until BDOI confirms them.

| Parameter | Default | Purpose |
|---|---|---|
| `MIG_ENVIRONMENT_CLASS` | `NON_PRODUCTION` | `PRODUCTION` switches masking off; any other value masks names, IDs and contacts at intake. |
| `MIG_CUTOVER_DATE` | `2028-01-03` | Go-live date T of the production plan. |
| `MIG_OPENING_VALUE_DATE` | `2028-01-01` | Value date of the opening entries of legacy invoices and unapplied payments and of the provisional trial balance. |
| `MIG_YEAR_END_OPTION` | `A` | Year-end option (section 17.7): `A` opens the provisional trial balance with the FY2027 result in retained earnings and posts the FY2027 true-ups; `B` opens the profit and loss accounts by account and refuses true-ups. Option A awaits Comptrollership's confirmation. |
| `MIG_STAGING_RETENTION_DAYS` | `5` | Days a signed-off batch keeps its staged payloads before `MIG_STAGING_PURGE`. |
| `MIG_CHUNK_SIZE`, `MIG_PARTITIONS` | `500`, `4` | Rows per load transaction and parallel partitions of a load. |
| `MIG_AMOUNT_TOLERANCE` | `0.00` | Largest amount difference a reconciliation line accepts. |
| `MIG_MAX_ERROR_RATE_MASTER`, `MIG_MAX_ERROR_RATE_FINANCIAL` | `0.5`, `0` | Largest share of failing or waived rows with which a master or a financial batch may be loaded. |
| `MIG_CLIENT_MATCH_AUTO`, `MIG_CLIENT_MATCH_REVIEW` | `90`, `60` | Client match scores for an automatic merge and for the Data Steward review queue. |
| `MIG_INVOICE_NO_COLLISION_PREFIX` | `true` | Prefix the source system to a legacy invoice number found in two systems. |
| `MIG_LEGACY_INVOICE_NO_PATTERN` | `^I\d{8}$` | Legacy invoice numbers the payment matcher recognises. |
| `MIG_UPP_ISSUE_AR` | `false` | Issue a BIBS acknowledgement receipt for each migrated unapplied payment. |
| `MIG_GOLIVE_RENEWAL_TO`, `MIG_RENEWAL_URGENT_TO` | `2028-05-31`, `2028-01-31` | Window of the go-live renewal extraction and of the urgent January renewals. |
| `MIG_LAST_LEGACY_BUSINESS_DAY`, `MIG_FREEZE_AT`, `MIG_RESUBMIT_DEADLINE` | `2027-12-29`, `2027-12-31 22:00`, `2028-01-02 12:00` | Cut-over calendar of the RA-sent file and its resubmissions. |
| `MIG_RA_MAX_LEAD_DAYS` | `140` | Oldest renewal advice date accepted in the RA-sent file. |
| `MIG_LEGACY_ACCESS_REASON_REQUIRED` | `true` | The Legacy Inquiry asks a reason once per session and logs it with every access. |
| `MIG_ARCHIVE_EXPORT_MAX_ROWS` | `1000` | Largest Excel export of the legacy archive. |
| `MIG_ACCESS_EXPORT_ALERT_ROWS` | `5000` | Archive records one user may export in a day before the alert `MIG_LEGACY_ACCESS_UNUSUAL`. |
| `MIG_LEGACY_LINK_EBIX`, `MIG_LEGACY_LINK_QPS` | blank | Addresses of the read-only legacy applications shown on the Legacy Inquiry. |

Legacy documents of the archive (object H02) are read through the port `LegacyDocumentSource`; until the transfer
folder is connected (DMQ24) they are staged in the console (`POST /api/v1/migration/archive/documents/{system}`,
permission `MIG_INTAKE`) under the short-lived record class `MIGRATION_EXTRACT`, and the archive keeps its own copy as
an attachment `LEGACY_DOCUMENT`.

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
| `BROKERVERSE_REDIS_TLS` | yes (`true`) | `true`; `false` in the `dev`, `test` and `seed` profiles | `spring.data.redis.ssl.enabled`: `true` with ElastiCache in-transit encryption. |
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
| `BROKERVERSE_LOGIN_RATE_WINDOW` | no | `PT1M` | `brokerverse.security.login-protection.rate-limit-window`. The limit covers every anonymous sign-in step: password, second-factor code and enrolment, completion of a single sign-on. |
| `BROKERVERSE_RESET_RATE_LIMIT` | no | `10` | `brokerverse.security.login-protection.reset-max-per-address`: "Forgot password?" requests (request, check and confirm of a link) accepted per client address and reset window (HTTP 429 `RESET_RATE_LIMITED` above). |
| `BROKERVERSE_RESET_LINKS_PER_USER` | no | `3` | `brokerverse.security.login-protection.reset-max-per-user`: reset links e-mailed for one user per reset window; further requests get the same answer and no e-mail. |
| `BROKERVERSE_RESET_RATE_WINDOW` | no | `PT15M` | `brokerverse.security.login-protection.reset-window`. |
| `BROKERVERSE_LOGIN_FAILURE_WINDOW` | no | `P1D` | `brokerverse.security.login-protection.failed-attempt-window`: life of the shared failed-login counter of a user. The lockout itself still follows `LOGIN_MAX_FAILED_ATTEMPTS`; `sec_user.failed_attempts` stays the record. |
| `BROKERVERSE_ATTACHMENT_MAX_SIZE` | no | `10MB` | `brokerverse.attachments.max-size`: largest file accepted as an attachment of a record (a Spring data size such as `10MB`); keep it at or below the multipart and ingress body limits. |
| `BROKERVERSE_KAFKA_ENABLED` | yes (`true`) | `true` | `brokerverse.kafka.enabled`. `true`: the outbox is relayed to Kafka and the consumers run (archive, e-mail dispatch, dead-letter recorder); `false`: events are recorded as delivered in-process (`LOCAL`) and e-mails are sent after commit and by `MAIL_DISPATCH`. |
| `BROKERVERSE_KAFKA_BOOTSTRAP_SERVERS` | when enabled | `localhost:9092` | `spring.kafka.bootstrap-servers`. On AWS: the Amazon MSK bootstrap brokers (SASL/SCRAM port 9096 or TLS port 9094). |
| `BROKERVERSE_KAFKA_SECURITY_PROTOCOL` | yes (`SASL_SSL`) | `SASL_SSL`; `PLAINTEXT` in the `dev`, `test` and `seed` profiles | `spring.kafka.properties.security.protocol`: `SASL_SSL` on MSK with SASL/SCRAM (production refuses anything else). |
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

## Runtime role, HTTPS and encryption in transit

Design: [`ARCHITECTURE_OPTION_DECISION.md`](../architecture/ARCHITECTURE_OPTION_DECISION.md) sections 2 and 5;
deployment of the four workloads: [`DEPLOYMENT.md`](DEPLOYMENT.md).

**Runtime role.** One image runs as `bibs-web`, `bibs-jobs` and `bibs-integration`, each with its own role.

| Role | HTTP | Scheduled jobs | Kafka consumers, outbox relay |
|---|---|---|---|
| `web` | user screens and APIs, actuator; `/integration/**` answers 404 | none (no task scheduler) | none; outbox rows wait for the relay of `bibs-integration` |
| `jobs` | actuator only (health, metrics); everything else 404 | batch jobs (every `ManagedJob` of workload `BATCH`) | none |
| `integration` | `/integration/**` and actuator | integration jobs: `EVENT_OUTBOX_RELAY`, `SCR_WATCHLIST_INGEST`, `QUOTATION_REQUEST_INTAKE` | consumers on, after-commit relay on |
| `all` (default) | everything | all | on (when Kafka is enabled) |

Manual runs from *Administration › Scheduled Jobs* execute on the instance that receives the request (a `bibs-web`
pod); the job lock still keeps one run at a time.

**Encryption in transit.** The defaults of `application.yml` are TLS for every connection; the `dev`, `test` and
`seed` profiles switch PostgreSQL, Redis and Kafka to plaintext unless the variables below ask otherwise, so a
seed stack on AWS (UAT) sets them explicitly (the Kubernetes base configuration does).

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `BROKERVERSE_RUNTIME_ROLE` | yes | `all` | `brokerverse.runtime.role`: `web`, `jobs`, `integration` or `all` (table above). Any other value stops the start. |
| `BROKERVERSE_SERVER_SSL_ENABLED` | yes (`true`) | `false` | `server.ssl.enabled`: HTTPS on `BROKERVERSE_PORT` with the PEM bundle `server` (`server.ssl.bundle`), TLS 1.3 and 1.2. The certificate is reloaded when the mounted files change (cert-manager rotation). |
| `BROKERVERSE_SERVER_SSL_CERTIFICATE` | with HTTPS | – | `spring.ssl.bundle.pem.server.keystore.certificate`, e.g. `file:/etc/bibs/tls/tls.crt` (certificate and chain). |
| `BROKERVERSE_SERVER_SSL_PRIVATE_KEY` | with HTTPS | – | `spring.ssl.bundle.pem.server.keystore.private-key`, e.g. `file:/etc/bibs/tls/tls.key`. |
| `BROKERVERSE_DB_SSL_MODE` | yes (`verify-full`) | `verify-full`; `prefer` in the `dev`, `test` and `seed` profiles | PostgreSQL driver `sslmode` (`spring.datasource.hikari.data-source-properties.sslmode`). Amazon RDS with `rds.force_ssl=1`. |
| `BROKERVERSE_DB_SSL_ROOT_CERT` | with `verify-*` | `/etc/bibs/rds-ca/global-bundle.pem` | PostgreSQL driver `sslrootcert`: the RDS CA bundle (`global-bundle.pem`, mounted from the ConfigMap `rds-ca-bundle`). |

`BROKERVERSE_REDIS_TLS` and `BROKERVERSE_KAFKA_SECURITY_PROTOCOL` are in the Redis and Kafka table above. SMTP uses
STARTTLS (`MAIL_SMTP_STARTTLS`, default `true`).

**API gateway tokens of `/integration/**`** (`IntegrationSecurityConfig`, a security chain separate from the user
chain). Only OAuth 2.0 access tokens issued by Apigee X are accepted there, and they are refused on the user APIs; a
user token issued by BIBS is refused on `/integration/**`. Stateless: no session, no cookie.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `BROKERVERSE_INTEGRATION_JWK_SET_URI` | on `integration` / `all` | – | `brokerverse.integration.security.jwk-set-uri`: HTTPS address of the Apigee JSON Web Key Set. Without it (or issuer / audiences) every `/integration` call is refused. |
| `BROKERVERSE_INTEGRATION_ISSUER` | on `integration` / `all` | – | Expected `iss` claim. |
| `BROKERVERSE_INTEGRATION_AUDIENCES` | on `integration` / `all` | – | Comma-separated accepted `aud` values (one must be present). |
| `BROKERVERSE_INTEGRATION_SCOPE_CLAIM` | no | `scope` | Claim carrying the scopes (space-separated string or list). |
| `BROKERVERSE_INTEGRATION_JWS_ALGORITHMS` | no | `RS256` | Accepted signature algorithms (asymmetric only). |
| `BROKERVERSE_INTEGRATION_CLOCK_SKEW` | no | `PT60S` | Tolerance on `exp` / `nbf`. |
| `BROKERVERSE_INTEGRATION_PING_SCOPES` | no | – (any valid token) | Scopes required by `GET /integration/v1/ping`, the connectivity check of an Apigee proxy (answers the client id and scopes of the token). |
| `BROKERVERSE_INTEGRATION_SECURITY_APIS_<NAME>_PATH` / `_SCOPES` | per API | – | `brokerverse.integration.security.apis.<name>.path` / `.scopes`: one rule per published API, a path pattern (e.g. `/integration/v1/receipts/**`) and the scopes the token must all carry. A path under `/integration/` without a rule is refused (403); the most specific matching pattern applies. |

Answers: no or invalid token (signature, issuer, audience, expiry) 401 with `WWW-Authenticate: Bearer`; valid token
without a required scope, or a path without a rule, 403.

## Authentication and session security

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `BROKERVERSE_MFA_ENCRYPTION_KEY` | every environment but local | – (a documented local value in docker compose, a test value in the test profile) | AES-256 key of the authenticator app secrets (second factor): 32 random bytes in Base64 (`openssl rand -base64 32`). Without it on a developer's machine the second factor is not asked. |
| `BROKERVERSE_MFA_PREVIOUS_ENCRYPTION_KEY` | no | – | The previous key during a rotation: secrets are read with either key and re-encrypted with the current one at their next use. |
| `BROKERVERSE_MANAGEMENT_PORT` | no (9090 in Kubernetes) | the application port | Port of the actuator (health, info, Prometheus metrics). On a separate port the metrics need no token; restrict the port to the monitoring namespace (NetworkPolicy `allow-monitoring`). `/livez` and `/readyz` stay on the application port for the probes and the load balancer. On the shared port `/actuator/prometheus` needs `METRICS_VIEW`. |
| `BROKERVERSE_MAIL_FROM` | every environment but local, when mail delivery is on | – (`no-reply@localhost` on a developer's machine) | Sender of the e-mails (`brokerverse.mail.from-address`); it takes precedence over the parameter `MAIL_FROM_ADDRESS`. |
| `BROKERVERSE_MIGRATION_MASKING_KEY` | no (the migration intake needs it outside production) | – (a documented local value in docker compose) | Keyed masking of personal data in non-production extracts; a development value is refused outside local. The `seed` profile has no default any more. |
| `BROKERVERSE_SSO_BASE_URL` | with single sign-on | – | Public address of BrokerVerse, e.g. `https://bibs.example.com`. Redirect URI to register at an OpenID Connect provider: `<base>/api/v1/auth/sso/oidc/callback`; SAML assertion consumer service: `<base>/api/v1/auth/sso/saml/acs`; SAML metadata: `<base>/api/v1/auth/sso/saml/metadata`. |
| `BROKERVERSE_SSO_LABEL` | no | `your organisation` | Name of the identity provider on the sign-in button. |
| `BROKERVERSE_SSO_BREAK_GLASS_USERS` | recommended with single sign-on | – | Comma-separated user names that keep a local password in OIDC or SAML mode (emergency administrators); the second factor is always asked of them. |
| `BROKERVERSE_SSO_USERNAME_CLAIM` | no | `preferred_username` (OIDC), the NameID (SAML) | Claim or attribute holding the BrokerVerse user name. The identity is linked to an existing, active user only; no user is ever created. |
| `BROKERVERSE_SSO_GROUPS_CLAIM` | no | – | Claim or attribute holding the user's groups, for the optional group check `brokerverse.security.sso.group-roles.<group>=<ROLE_CODE>` (set in a deployment file): a single sign-on is accepted only when the provider asserts a group mapped to a role the user holds. Roles are never granted from the groups. |
| `BROKERVERSE_SSO_REQUIRE_LOCAL_MFA` | no | `false` | `true` also asks the BrokerVerse second factor after a single sign-on (normally the provider enforces its own). |
| `BROKERVERSE_SSO_CLOCK_SKEW` | no | `PT2M` | Tolerance on the validity times of ID tokens and assertions. |
| `BROKERVERSE_OIDC_ISSUER` | with `AUTH_MODE=OIDC` | – | Issuer of the OpenID Connect provider; the endpoints are read from `<issuer>/.well-known/openid-configuration` unless set below. |
| `BROKERVERSE_OIDC_CLIENT_ID` / `BROKERVERSE_OIDC_CLIENT_SECRET` | with `AUTH_MODE=OIDC` | – | Confidential client registered at the provider (`client_secret_basic`); the secret from the secret store. |
| `BROKERVERSE_OIDC_SCOPES` | no | `openid profile email` | Scopes asked for. |
| `BROKERVERSE_OIDC_AUTHORIZATION_URI`, `BROKERVERSE_OIDC_TOKEN_URI`, `BROKERVERSE_OIDC_JWK_SET_URI` | no | discovered | Endpoints when the provider has no discovery document. |
| `BROKERVERSE_OIDC_JWS_ALGORITHM` | no | `RS256` | The only signature algorithm accepted on ID tokens. |
| `BROKERVERSE_SAML_IDP_ENTITY_ID` | with `AUTH_MODE=SAML` | – | Entity id of the SAML provider (the Issuer of its responses). |
| `BROKERVERSE_SAML_IDP_SSO_URL` | with `AUTH_MODE=SAML` | – | Single sign-on address of the provider (HTTP-Redirect binding). |
| `BROKERVERSE_SAML_IDP_CERTIFICATE` | with `AUTH_MODE=SAML` | – | The provider's signing certificate or public key (PEM text, or `file:` path to it); the key inside a message is never trusted. |
| `BROKERVERSE_SAML_SP_ENTITY_ID` | no | `BROKERVERSE_SSO_BASE_URL` | Entity id of BrokerVerse at the provider (the audience of the assertions). |
| `BROKERVERSE_SAML_NAME_ID_FORMAT` | no | unspecified | NameID format asked for. |

Business parameters of the sign-in (security parameters: a change waits for a second approval):

| Parameter | Delivered | Purpose |
|---|---|---|
| `AUTH_MODE` | `LOCAL` | `LOCAL`, `DIRECTORY`, `OIDC` or `SAML`. Switch to `OIDC` or `SAML` only once the provider is configured and a break-glass administrator exists. |
| `MFA_POLICY` | `PRIVILEGED` (`OFF` in the SIT/UAT seed data) | Who must confirm a sign-in with an authenticator app code: `ALL`, `PRIVILEGED` (a role of privilege level HIGH or ADMIN) or `OFF`. A user who enrolled is always asked. |
| `MFA_REMEMBER_DEVICE_DAYS` | `0` | Days a device may be remembered after a confirmed code (0 to 30; 0 = never). |
| `MFA_ISSUER_NAME` | `iNXT BrokerVerse` | Name shown next to the account in the authenticator app. |
| `ACCESS_TOKEN_MINUTES` | `15` | Life of an access token (5 to 60). |
| `SESSION_TIMEOUT_MINUTES` | (existing) | Inactivity after which a session ends; the renewal of the token stops as well. |
| `LOGIN_MAX_FAILED_ATTEMPTS` | (existing) | Failed sign-ins (password or second-factor code) that lock an account. |

## Start-up safeguards

`ProductionSafeguards` (package `config`, registered in `META-INF/spring.factories`) checks the resolved
configuration before any bean is created. **Every environment except local** (`BROKERVERSE_ENVIRONMENT` other than
`local`, or the `prod` profile) gets the secret checks; a production start (the `prod` profile or
`BROKERVERSE_ENVIRONMENT=production`; the `prod` profile sets `production` itself) gets the transport and
integration checks as well. BIBS refuses to start, and lists every problem in one message, when:

- (every environment but local) `BROKERVERSE_ALLOWED_ORIGINS` or `BROKERVERSE_PASSWORD_RESET_URL` is missing or
  names localhost; `BROKERVERSE_MFA_ENCRYPTION_KEY` is missing, not 32 bytes of Base64 or a development value; mail
  delivery is on and `BROKERVERSE_MAIL_FROM` is missing; a masking key is a development value; an OpenID Connect
  issuer is set without `BROKERVERSE_OIDC_CLIENT_ID` / `_CLIENT_SECRET`, or a SAML provider without its
  certificate. The API documentation (springdoc) is switched off outside local and the `dev` profile;

- the `seed` profile is active in production (seed data never loads in production);
- `BROKERVERSE_DB_URL`, `BROKERVERSE_DB_USER` or `BROKERVERSE_DB_PASSWORD` is missing (the `prod` profile has no
  defaults for them);
- `SPRING_FLYWAY_USER` or `SPRING_FLYWAY_PASSWORD` is missing, or `SPRING_FLYWAY_USER` names the same login as
  `BROKERVERSE_DB_USER` (the application must connect as the least-privilege runtime login, not as the schema owner;
  not checked when the migrations are switched off with `SPRING_FLYWAY_ENABLED=false`);
- (every environment but local) `BROKERVERSE_JWT_SECRET` is missing, shorter than 32 characters or a development
  value; the database, SMTP, Redis and Kafka credentials below are missing;
- mail delivery is on (`BROKERVERSE_MAIL_ENABLED=true`) and `MAIL_HOST`, or with SMTP authentication
  `MAIL_USERNAME` / `MAIL_PASSWORD`, is missing;
- Redis is on (`BROKERVERSE_REDIS_ENABLED`, default `true`) and `BROKERVERSE_REDIS_PASSWORD` is missing;
- Kafka is on (`BROKERVERSE_KAFKA_ENABLED`, default `true`) and `BROKERVERSE_KAFKA_SASL_JAAS_CONFIG` is missing;
- a connection could run in plaintext: PostgreSQL `sslmode` (URL or `BROKERVERSE_DB_SSL_MODE`) other than
  `verify-full` / `verify-ca`, Redis on without TLS (`BROKERVERSE_REDIS_TLS`), Kafka on with a protocol other than
  `SASL_SSL`, or the HTTP listener without TLS (`BROKERVERSE_SERVER_SSL_ENABLED`, and then
  `BROKERVERSE_SERVER_SSL_CERTIFICATE` / `_PRIVATE_KEY` missing);
- the instance serves `/integration/**` (runtime role `integration` or `all`) and `BROKERVERSE_INTEGRATION_JWK_SET_URI`,
  `BROKERVERSE_INTEGRATION_ISSUER` or `BROKERVERSE_INTEGRATION_AUDIENCES` is missing, or the runtime role is not one
  of `web`, `jobs`, `integration`, `all`.

The base `application.yml` holds no password or signing key, and neither does the `seed` profile (it reads the JWT
key, the masking key and the second-factor key from the environment like every profile). Only the automated tests
and the local docker compose stack carry documented local values; the `seed` profile is refused in production.

## Document storage (S3, build step ST0)

Design and as-built description: [`DOCUMENT_STORAGE_DECISION.md`](../architecture/DOCUMENT_STORAGE_DECISION.md)
sections 3 to 6. File content lives in S3. BIBS keeps the metadata (`stored_file`) and issues short-lived presigned
links. All properties are under `brokerverse.storage.*`.

| Variable | Required in prod | Default | Purpose |
|---|---|---|---|
| `BROKERVERSE_STORAGE_PROVIDER` | yes (`s3`) | `local` | `brokerverse.storage.provider`. `s3`: Amazon S3 with SSE-KMS; the start is refused without the bucket and KMS key of the documents, reports and inbound classes. `local`: file system for developer machines, automated tests and seed stacks; refused when `BROKERVERSE_ENVIRONMENT=production`. |
| `BROKERVERSE_STORAGE_REGION` | yes | `ap-southeast-1` | `brokerverse.storage.region`: AWS region of the buckets. It must be an approved region: Singapore for the Philippine data; the DR region depends on DSQ01. |
| `BROKERVERSE_STORAGE_BUCKET_DOCUMENTS` | with `s3` | – | `brokerverse.storage.buckets.documents`, e.g. `bibs-prod-documents`. Versioning, Object Lock (governance mode) and GuardDuty on the upload prefix. |
| `BROKERVERSE_STORAGE_BUCKET_REPORTS` | with `s3` | – | `brokerverse.storage.buckets.reports` (report runs, scheduled files, batch ZIPs). |
| `BROKERVERSE_STORAGE_BUCKET_INBOUND` | with `s3` | – | `brokerverse.storage.buckets.inbound` (bulk and bank or insurer files; GuardDuty enabled). |
| `BROKERVERSE_STORAGE_BUCKET_MIGRATION` | no | – | `brokerverse.storage.buckets.migration` (migration extracts; expire after 5 days; migration role only). |
| `BROKERVERSE_STORAGE_KMS_DOCUMENTS` / `BROKERVERSE_STORAGE_KMS_REPORTS` / `BROKERVERSE_STORAGE_KMS_INBOUND` / `BROKERVERSE_STORAGE_KMS_MIGRATION` | with `s3` (the first three) | – | `brokerverse.storage.kms-keys.*`: ARN of the BDOI-owned customer-managed KMS key of each bucket class (SSE-KMS, bucket keys on). The key policy gives the BIBS service role encrypt, decrypt and generate-data-key only. Imported key material or an external key store needs no change to BIBS. |
| `BROKERVERSE_STORAGE_ENDPOINT` | no (empty on AWS) | – | `brokerverse.storage.endpoint`: an S3-compatible endpoint for tests only. On AWS traffic goes through the S3 VPC gateway endpoint without an override. |
| `BROKERVERSE_STORAGE_PATH_STYLE` | no | `false` | `brokerverse.storage.path-style-access` (S3-compatible services). |
| `BROKERVERSE_STORAGE_LOCAL_ROOT` | no | `<tmp>/brokerverse-files` | `brokerverse.storage.local.root`: folder of the local store. |
| `BROKERVERSE_STORAGE_LOCAL_LINK_SECRET` | no | random per start | `brokerverse.storage.local.link-secret`: key that signs the links of the local store. Set it when several instances share one local folder. |
| `BROKERVERSE_STORAGE_LOCAL_SCAN_STATUS` | no | `NO_THREATS_FOUND` | `brokerverse.storage.local.scan-status`: scan result the local store gives every object (it marks files clean). |
| `BROKERVERSE_STORAGE_MAX_UPLOAD_SIZE` | no | `25MB` | `brokerverse.storage.max-upload-size`: largest file accepted through the application. Larger bulk files go by presigned PUT (up to 5 GB). `BROKERVERSE_UPLOAD_MAX_FILE_SIZE` / `BROKERVERSE_UPLOAD_MAX_REQUEST_SIZE` (`spring.servlet.multipart.*`, now `25MB` / `26MB`) and the ingress body limit must allow it. |
| `BROKERVERSE_STORAGE_LINK_TTL` | no | `PT5M` | `brokerverse.storage.link-ttl`: fallback for the business parameter `FILE_LINK_TTL_SECONDS` (seeded 300, range 30-3600, *Administration › Parameters*), which sets the validity of presigned links. |
| `BROKERVERSE_STORAGE_ORPHAN_AGE` | no | `PT24H` | `brokerverse.storage.orphan-age`: objects without a metadata row older than this are deleted by `FILE_ORPHAN_RECONCILIATION`. Announced uploads not confirmed within it are closed. |
| `BROKERVERSE_STORAGE_DELETED_GRACE` | no | `P30D` | `brokerverse.storage.deleted-grace`: time between the soft delete of a file and the removal of its object by `FILE_RETENTION`. |
| `BROKERVERSE_MALWARE_SCAN` | yes (`storage`) | `storage` | `brokerverse.attachments.malware-scan`, the explicit malware scan decision of uploaded files, checked at start-up (`MalwareScanPolicy`). `storage`: the file store scans every object (S3 with GuardDuty Malware Protection; a file cannot be downloaded before its result is `NO_THREATS_FOUND`, infected files are quarantined); in production the start is refused unless `BROKERVERSE_STORAGE_PROVIDER=s3`. `application`: an antivirus scanner bean of the deployment scans each file before it is stored; the start is refused when only the built-in no-op scanner is present. `none`: no scan, development and automated tests only; refused in production. A deployed scanner bean scans in every mode, in addition to the storage scan. |
| `BROKERVERSE_STORAGE_SCAN_TAG` | no | `GuardDutyMalwareScanStatus` | `brokerverse.storage.scan-tag`: object tag with the malware scan result. Only `NO_THREATS_FOUND` is accepted. |
| `BROKERVERSE_JOB_FILE_SCAN_RESULTS_CRON` | no | `0 */5 * * * *` | Spring cron (UTC) of `FILE_SCAN_RESULTS` (every 5 minutes): reads the scan results of pending files and quarantines infected ones (notification to the uploader and to `FILE_QUARANTINE_VIEW`, alert `FILE_QUARANTINED`). Property `brokerverse.storage.jobs.scan-results-cron`. |
| `BROKERVERSE_JOB_FILE_ORPHAN_CRON` | no | `0 10 2 * * *` | Spring cron (UTC) of `FILE_ORPHAN_RECONCILIATION` (daily): deletes objects that have had no row for 24 hours and are not under legal hold. Property `brokerverse.storage.jobs.orphan-cron`. |
| `BROKERVERSE_JOB_FILE_RETENTION_CRON` | no | `0 40 2 * * *` | Spring cron (UTC) of `FILE_RETENTION` (daily): removes the objects of files past retention (record class mapped to the retention rules) or deleted longer than the grace period. Files under legal hold are kept and counted. Property `brokerverse.storage.jobs.retention-cron`. |
| `BROKERVERSE_JOB_FILE_ECM_ARCHIVE_CRON` | no | `0 */15 * * * *` | Spring cron (UTC) of `FILE_ECM_ARCHIVE` (every 15 minutes): publishes final records of the "archive to ECM" classes to `bibs.storage.ecm-archive-requested.v1`. Property `brokerverse.storage.jobs.ecm-archive-cron`. |
| `BROKERVERSE_STORAGE_DOWNLOAD_MODE` | no | `stream` | How the download endpoints of the modules answer for a stored file (build step ST1): `redirect` sends the browser to the presigned link, so the bytes never pass through BIBS; `stream` reads the file through BIBS with its SHA-256 re-checked. Set `redirect` in an S3 environment only once the buckets allow the web origin (CORS: `GET`, exposed `Content-Disposition` and `Content-Type`) and the web client's `connect-src` lists the bucket hosts. With the local store (`local`) `redirect` works as is. `GET /api/v1/files/{id}/link` always returns the link. Property `brokerverse.storage.downloads.mode`. |
| `BROKERVERSE_JOB_FILE_BYTEA_MIGRATION_CRON` | no | `-` (manual) | Spring cron (UTC) of `FILE_BYTEA_MIGRATION`, the one-off copy of the files still kept in the database to the file store (build step ST1). Keep it manual and run it from the job monitor until the reconciliation report (`GET /api/v1/files/content-migration`) shows nothing left. Property `brokerverse.storage.content-migration.cron`. |
| `BROKERVERSE_FILE_BYTEA_MIGRATION_BATCH` | no | `20` | Rows read per query by `FILE_BYTEA_MIGRATION` (1 to 500). Each row is copied and checked in its own transaction. Property `brokerverse.storage.content-migration.batch-size`. |

**Content checks of uploaded files.** Every Office Open XML and OpenDocument file opened from outside (bulk and
journal uploads, protected e-mail attachments, edited Word templates) is read within fixed decompression limits
(`OfficeFileLimits`): at most 1,000 parts, 64 MB for one uncompressed part and an expansion of at most 100 times the
compressed size; a larger file is refused as a possible compressed-file bomb (`FILE_ARCHIVE_LIMIT`). An OpenDocument
sheet expands to at most 100,000 rows of 1,024 columns. CSV, TXT and EML files must be text throughout (no NUL byte
and no binary control character anywhere in the file); CSV and TXT files read as data (bulk and journal uploads)
must also be UTF-8 (`FILE_TEXT_ENCODING`, with a hint to save as "CSV UTF-8") and at most 25 MB.

**Request size.** `BROKERVERSE_MAX_JSON_BODY_SIZE` (`brokerverse.http.max-json-body-size`, default `2MB`) limits a
JSON request body: a larger declared length is answered with 413 before security and the controllers run, and a
body without a length stops being read at the limit (413, `REQUEST_TOO_LARGE`). File uploads keep the multipart
limits above.

**Log forging.** Line breaks in log messages (CR, LF, NEL, Unicode line separators) are replaced by `_` by the
message converter of `logback-spring.xml` (`%m`, `%msg`, `%message`), so the protection holds for any
`LOGGING_PATTERN_CONSOLE` and any file appender added to that file; structured JSON logging escapes them by format.
BIBS logs to the console only (the platform collects container output).

Credentials are never configured. On EKS the pod uses the IAM role of its service account (IRSA) through the AWS
default credential chain. For the S3 adapter's own test on a developer machine, the standard `AWS_*` variables can
be used.

The storage users of the seed data (`holdofficer`, `holdapprover`, `infosec`) are in `db/seed/V1109`, which is in
the document storage range V1100-V1109.

## Seed data (SIT, UAT and training)

The `seed` profile adds the Flyway location `classpath:db/seed` (versions V900-V999 and V1900-V1999) and the seed
start-up runners (`*.seed` packages, `*SeedData` classes). It creates the seed company FVI under the client's legal
name, BDO Insurance and Reinsurance Brokers, Inc., its branches, chart of accounts, seed records and the SIT/UAT
users. Seed record numbers are plain sequence numbers in the 9000xx range (for example `CL-2026-900001`,
`AR-2026-900001`). The SIT/UAT password is provided to testers separately; it is written in no file of the project,
the client documents included (a standing test refuses it, `SeedPasswordGuardTest`).

- **Password per environment.** The seed scripts give every SIT/UAT user one shared password hash. Set
  `BROKERVERSE_SEED_PASSWORD` in the secret store of each seed environment: at start, `SeedPasswords` (seed profile,
  first runner) gives that password to every user still carrying the hash of the seed scripts, so the hash of the
  scripts opens no account there; with `BROKERVERSE_SEED_PASSWORD_MUST_CHANGE=true` the users change it at their
  first sign-in. Users with another password are left alone, so later starts change nothing; to issue a new
  password, reset the users on the User Access screens or recreate the seed database. Without the variable the users
  keep the hash of the scripts and the start logs a warning. The `upphandler` persona created at start takes the
  password of `badmin`.
- **Automated tests** never use the SIT/UAT password: `SignInPasswords` (test support) gives the users a test signs
  in as a random password drawn for the test run.

- **Not yet applied anywhere.** The seed migrations were renamed to `db/seed/V9xx__seed_*.sql` and
  `V19xx__seed_*.sql` (with their record codes and names) before any environment applied them, so no Flyway history
  refers to the former names. A database created from an earlier local build is dropped and recreated; `flyway
  repair` is not needed on SIT, UAT or production.
- **Comment-only edits of schema migrations.** The same change reworded comments of some `db/migration` files
  (V652, V764, V771, V870, V880, V890, V1000, V1020, V1050-V1052, V1055, V1060). No SQL statement changed, but the Flyway
  checksums did: a database migrated by an earlier build runs `flyway repair` once (or is recreated) before the
  next start.
- **Release note: the SIT/UAT password left the seed scripts.** The header comments of 16 seed scripts (V900, V980,
  V990, V998, V999, V1109, V1900, V1910, V1920, V1930, V1940, V1950, V1952, V1960, V1970, V1980) no longer state the
  SIT/UAT password; it is provided to testers separately. No SQL statement changed, but the Flyway checksums did: a
  database that applied these seed scripts (seed environments only: local stacks, SIT, UAT and training) runs
  `flyway repair` once, or is recreated, before its next start. Never run it on production, which never applies
  `db/seed`. `spring.flyway.validate-on-migrate` keeps its default (on) in every profile; the automated tests build
  a fresh database on every run and need no repair.

## Client-specific values (client profile, business rules, theme pack)

iNXT BrokerVerse is a product for many clients: no client name, code, logo, currency or business
term is written into the platform code. A deployment sets them in data and in its theme pack. The
standing checks `ClientNeutralityTest` (backend build) and the `no-restricted-syntax` rule of
`frontend/eslint.config.js` (`npm run lint`) refuse client names and currency codes in platform
code; seed data, tests and the theme packs are exempt.

### Client profile (company master, V1150)

Maintained on *Setup › Companies*: select a company to open its **Client profile** (the change
returns the company to pending authorization). The legal name, address, TIN and base currency are
the company's own fields.

| Field | Purpose |
|---|---|
| Short name | Name of the company in texts and labels ("Via …", "… Only", "Renew with …"); the company code when blank. |
| Group name | Group the company belongs to, used in labels of group concepts ("… Bank Client", "… CIF number"); blank = none. |
| Document logo | Logo on documents: `theme:logo` (or blank) = the logo of the deployed theme pack. |
| Head office code | Code of the head office in files exchanged with the client (DP lists `<code>_DP_<yyyyMMdd>`, collection hand-offs) and for records without a branch (incentive pass-on); the code of the head-office branch when blank. |
| Default bank account | Bank account code proposed where a report asks for the company account (Payment Notification to the Bank). |

The base currency of the company is used wherever a record or an uploaded file carries no currency
(accounts, quotations, PRFs, payments, checks, fees, targets, renewals, submitted policies), in the
examples of the upload templates downloaded for the company, and on the screens (proposed currency,
amount headings); the currencies offered in lists come from the currency master.

### Business rules as parameters (V1152)

Seeded with the values the platform used before; changed on *Administration › Parameters*. A
missing value stops the action with `PARAMETER_NOT_SET` instead of falling back to a coded value.

| Parameter | Seeded | Used by |
|---|---|---|
| `CLIENT_CREDIT_DAYS` | 30 | credit days of the accounting party created for a client |
| `SERVICE_INVOICE_CREDIT_DAYS` | 30 | credit days merged into the service invoice text |
| `INSURER_DEFAULT_CREDIT_DAYS` | 30 | insurers loaded by data migration without credit days |
| `PASSWORD_EXPIRY_NOTICE_DAYS` | 7 | job `PASSWORD_EXPIRY_NOTICE` (SECURITY category: second approval) |
| `RATE_EXCEPTION_VALIDITY_DAYS` | 30 | end date of a rate-scheme exception requested without one |
| `DP_PREMIUM_TOLERANCE` | 1.00 | direct payment list check against the booked gross premium |
| `EWT_RATE_TOLERANCE` | 0.05 | EWT worksheet: document rate against the ATC rate (percentage points) |
| `OPEN_COVER_TRANSIT_DAYS` | 60 | transit days of an open-cover declaration without them |

The input VAT rate of supplier invoices is the rate of the company's active `VAT_INPUT` tax code on
the invoice date (*Tax › Tax codes*); without one, a VAT-registered invoice is refused with
`NO_INPUT_VAT_CODE`.

Kept in code after review (technical limits, scales, algorithm constants and fallbacks of seeded
parameters, not business rules): field lengths and list sizes (`MAX_TEXT`, `MAX_ERROR`,
`MAX_ROWS`…), rate scales (`RATE_SCALE`), percent bounds (`MAX_RATE`/`MAX_PERCENT` = 100), date
range limits of files and screens (payment notification at most 31 days, `MAX_TERM_YEARS` 10,
`MAX_FOLLOW_UP_DAYS` 365, `MAX_CREDIT_DAYS` 365), the client matching scores of data migration, the
letter catch-up window of submitted policies (7 days after a missed run), and the `DEFAULT_*`
fallbacks of parameters and exception-code thresholds that the platform seeds (for example
`QUOTATION_SLIP_REPLY_DAYS`, `HOLD_COVER_DAYS`, `DISB_STALE_DAYS`, `CWT_APPLICATION_PERCENT`).

### Theme pack and business zone

| Variable | Default | Purpose |
|---|---|---|
| `VITE_THEME_PACK` (frontend build) | `bdoi` | Theme pack of the web application, `frontend/src/theme/packs/<pack>`: `theme.json` (product and client names, page title and description, print title), `tokens.css` (brand colours and fonts), `index.ts` (fonts, logo, sign-in photo), `favicon.svg`. Build argument of `frontend/Dockerfile` and of the `frontend` service of `docker-compose.yml`. A new client gets a new pack; components never name the client. |
| `BROKERVERSE_THEME` (backend) | `bdoi` | Theme pack of generated files (reports and documents in PDF, Word, Excel): `backend/src/main/resources/theme/<pack>/brand.properties` (colours, font, logo size) and its logo. System property `brokerverse.theme` takes precedence. Read at start-up. |
| `BROKERVERSE_BUSINESS_ZONE` | `Asia/Manila` | See the first table. The zone is read from `BusinessClock` where it is used and bound as a parameter of SQL that takes the business day of a timestamp; no code keeps its own copy. |
