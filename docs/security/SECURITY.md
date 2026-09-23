# Security Controls

| Area | Control | Where |
|---|---|---|
| Authentication | BCrypt (cost 12) passwords; JWT HS256 bearer tokens with issuer and expiry; user re-loaded on every request so disabling/locking is immediate | `security`, `SecurityConfig` |
| Password policy | ≥ 10 chars, upper, lower, digit, symbol | `PasswordChangeRequest` |
| Lockout | 5 consecutive failures lock the account; unlock by administrator; all attempts audited | `AuthService` |
| Authorization | Fine-grained permissions (`Permission`) on every endpoint via `@PreAuthorize`; roles are permission bundles; report catalogue filtered per permission | controllers, `ReportService` |
| Segregation of duties | Administrators cannot post; makers cannot authorize their own journals or master data; authorization limits per user | `AuthorizableEntity`, `JournalBatch.authorize`, `JournalAuthorizationService` |
| Data integrity | Ledger rows immutable (DB trigger); corrections by reversal only; optimistic locking; balanced-journal invariant; gapless document numbers | V1 migration, `JournalBatch`, `DocumentNumberService` |
| Audit | Every create/update/authorize/post/reverse/login/report run/export recorded with user and time; insert-only | `audit` |
| Input validation | Bean Validation on all requests; typed parameters; SQL via bound parameters only | DTOs, repositories |
| Output | RFC 7807 errors; no stack traces or internal messages leaked; CSV formula-injection protection | `GlobalExceptionHandler`, `CsvReportRenderer` |
| Transport & headers | TLS at ingress; CSP, X-Frame-Options, Referrer-Policy, nosniff; CORS allow-list | nginx.conf, `SecurityConfig` |
| Secrets | Only from environment/secret store; no default admin credentials in production | `AdminBootstrap`, CONFIGURATION.md |
| Logging | CR/LF neutralised (log forging); no passwords in logs (`toString` masked) | application.yml, DTOs |
| Static analysis | SpotBugs + FindSecBugs, CodeQL, SonarJS | CI |
| Containers | Non-root runtime user, health checks, minimal JRE / nginx alpine images | Dockerfiles |

Reporting a vulnerability: contact the IortaTechNXT security team; do not open public issues.
