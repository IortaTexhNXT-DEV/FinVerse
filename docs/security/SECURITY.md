# Security Controls

| Area | Control | Where |
|---|---|---|
| Authentication | BCrypt (cost 12) passwords; JWT HS256 bearer tokens with issuer and expiry; user re-loaded on every request so disabling/locking is immediate | `security`, `SecurityConfig` |
| Password policy | ≥ 10 chars, upper, lower, digit, symbol | `PasswordChangeRequest` |
| Lockout | Consecutive failed logins up to the business parameter `LOGIN_MAX_FAILED_ATTEMPTS` (3 for BDOI, maintained under System Parameters; `BROKERVERSE_MAX_FAILED_ATTEMPTS`, default 5, only when the parameter is missing) lock the account for all users; unlock by an administrator; all attempts audited | `AuthService`, V1000, V1060 |
| Authorization | Fine-grained permissions (`Permission`) on every endpoint via `@PreAuthorize`; roles are permission bundles; report catalogue filtered per permission | controllers, `ReportService` |
| Segregation of duties | Administrators cannot post; makers cannot authorize their own journals or master data; authorization limits per user on journals, payables, claims and underwriting approvals (policies, endorsements, quotations), and the approval inbox hides items above the viewer's limit | `AuthorizableEntity`, `JournalBatch.authorize`, `JournalAuthorizationService`, `PayablesSupport`, `ClaimSupport`, `UnderwritingAuthority` |
| Data integrity | Ledger rows immutable (DB trigger); corrections by reversal only; optimistic locking; balanced-journal invariant; gapless document numbers | V1 migration, `JournalBatch`, `DocumentNumberService` |
| Audit | Every create/update/authorize/post/reverse/login/report run/export recorded with user and time, in the same transaction as the change; insert-only in the application (no update/delete API) **and** in the database: triggers reject every `UPDATE`, `DELETE` and `TRUNCATE` on `audit_log` for every client. There is no retention purge; archiving needs the table owner to disable the triggers, on record | `audit`, V26 migration |
| Input validation | Bean Validation on all requests; typed parameters; SQL via bound parameters only | DTOs, repositories |
| Output | RFC 7807 errors; no stack traces or internal messages leaked; CSV formula-injection protection | `GlobalExceptionHandler`, `CsvReportRenderer` |
| Transport & headers | TLS at ingress; CSP, X-Frame-Options, Referrer-Policy, nosniff; CORS allow-list | nginx.conf, `SecurityConfig` |
| Secrets | Only from environment/secret store; no default admin credentials in production | `AdminBootstrap`, CONFIGURATION.md |
| Logging | CR/LF neutralised (log forging); no passwords in logs (`toString` masked) | application.yml, DTOs |
| Static analysis | SpotBugs + FindSecBugs, SAST, SonarJS | CI pipeline |
| Containers | Both images run as a non-root user: the backend as `brokerverse` (JRE alpine), the frontend as the unprivileged nginx user listening on port 8080; health checks on both; minimal alpine base images | `backend/Dockerfile`, `frontend/Dockerfile` |

Reporting a vulnerability: contact the IortaTechNXT security team; do not open public issues.
