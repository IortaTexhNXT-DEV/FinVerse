# BDOI Application Security Requirements: BIBS checklist

Self-assessment of BIBS against the BDO Application Security Requirement workbook (ITS-SQT-1.2.8-L3,
ASR V4.10, Fraud-ASR V4.9). The requirement column summarises BDO's text; the workbook stays the
reference for the wording and the risk level. Evidence names the code, tests, configuration and
documents of this repository; `SECURITY_SELF_ASSESSMENT.md` holds the OWASP view of the same controls.

**Application profile.** Internal back-office web application of BDO Insurance Brokers (not public
or customer facing), with web services / APIs (internal integrations through the API gateway),
batch jobs, a PostgreSQL database and S3 document storage, deployed as containers on AWS. No mobile
application, no card data, no financial or payment services to customers, no AI component.
In production users sign in through Microsoft Entra ID (single sign-on, second factor at Entra ID);
local passwords exist only for test accounts outside production (`ProductionSafeguards`). Where the
workbook allows a password requirement to be met through the BDO directory, the status reflects
that, and the local test-account policy is noted.

Status: **Met**, **Partly** (met in part, or met by configuration still to be confirmed), **Not met**,
**N/A** (the component or exposure does not exist in BIBS).

## Summary

| Sheet | Met | Partly | Not met | N/A | Total |
|---|---:|---:|---:|---:|---:|
| ASR | 79 | 12 | 1 | 41 | 133 |
| Fraud-ASR | 0 | 0 | 0 | 27 | 27 |
| AI-ASR, AI-GOV | - | - | - | all | - |

## ASR - 1 General application requirements

| Req. | Requirement (summary) | Status | Evidence / remark |
|---|---|---|---|
| 1.1.1 | Sign-in with user ID and password before access; MFA for public-facing applications | Met | Entra ID single sign-on (`SingleSignOnIT`); test accounts: local password and TOTP second factor (`MfaPolicy`, `SecondFactorIT`) |
| 1.1.2 | PIN / biometrics only with strong secondary validation | N/A | No PIN or biometric sign-in |
| 1.1.3 | Password masked while typed | Met | Password fields of the sign-in and change screens (`type="password"`) |
| 1.1.4 | Sign-in errors give no clue | Met | One answer and timing for unknown, locked, disabled user and wrong password (`AuthService.check`, `LocalPasswordAuthenticator.INVALID`) |
| 1.2.1 | Change of temporary password at first sign-in and after reset | Met | Entra ID; test accounts: `must_change_password` set on creation and administrator reset (`UserAdminService.resetPassword`) |
| 1.2.2 | Stored password never clear text | Met | BCrypt hashes (`PasswordEncoder`); Entra ID in production |
| 1.2.3 | Password change every 90 days (optional under 2FA, lockout, complexity, disable facility) | Met | Entra ID; test accounts: `PASSWORD_MAX_AGE_DAYS` = 90 with notice (`PASSWORD_EXPIRY_NOTICE_DAYS`) |
| 1.2.4 | Access disabled after three invalid tries | Met | Entra ID; test accounts: `LOGIN_MAX_FAILED_ATTEMPTS` = 3 (V1060), sign-in rate limit (`LoginRateLimitFilter`) |
| 1.2.5 | Minimum length 8; 12 for administrator accounts | Partly | Entra ID policy in production. Test accounts: 10 characters for every account (`PasswordChangeRequest`), so administrator test accounts are below 12. The BDO Password Protection Guidelines (Sep 2026) ask 15 for single-factor and privileged access |
| 1.2.6 | Complex passwords (letters, digits, symbols; upper and lower case) | Met | `PasswordChangeRequest.COMPLEXITY` |
| 1.2.7 | Last eight passwords not reused | Met | `PASSWORD_HISTORY_COUNT` = 10 (`AuthPasswordPolicy`; set by migration V2700), so the last ten passwords are refused. This also meets the 10 the 2026 guidelines ask for internal systems |
| 1.2.8 | Minimum password age of one day | Met | `PASSWORD_MIN_AGE_DAYS` = 1 (`AuthPasswordPolicy`) |
| 1.2.9 | Old password and confirmation required to change | Met | `AuthController.changePassword` checks the current password; new password entered twice on the screen |
| 1.2.10 | Reset verifies the requester out of band | Met | `PASSWORD_RESET_BDOI_RULES` = true: no self-service link; reset only by another System Administrator; Entra ID in production |
| 1.2.11 | Customers notified of authentication changes | N/A | Not customer facing |
| 1.2.12 | Cooling-off after key account changes | N/A | Not customer facing; user details come from UIDM-ISC |
| 1.2.13 | User ID and password input length and characters checked | Met | `LoginRequest` (50 / 100 characters), `USER_ID_PATTERN` for joiners |
| 1.2.14 | Password parameters adjustable to the threat level | Partly | Expiry, history, minimum age, lockout, session and second factor are business parameters (User Access › Settings); the minimum length is fixed in code (10) |
| 1.3.1 | One-time codes expire; out-of-band codes after 5 minutes | Met | TOTP codes in 30-second steps (`Totp.STEP_SECONDS`); no out-of-band codes or links in BIBS (`PASSWORD_RESET_BDOI_RULES`) |
| 1.3.2 | Public-facing: out-of-band codes single use | N/A | Not public facing |
| 1.3.3 | Public-facing: out-of-band channel secure and independent | N/A | Not public facing |
| 1.3.4 | Customer-facing: identity proofing before MFA changes | N/A | Not customer facing |
| 1.3.5 | Second factor not sent on the reset channel | Met | Authenticator app (TOTP); nothing sent by e-mail or SMS |
| 1.4.1 | User administration: create, update, delete, lock / unlock | Met | Users from UIDM-ISC (SCIM, identity events); unlock, disable, end of sessions on User Access › Users (`UserAdminController`) |
| 1.4.2 | Role-based access control | Met | Group profiles and permissions (`Permission`, `@PreAuthorize`; `PermissionConsistencyTest`) |
| 1.4.3 | Custom roles for granular access | Met | Group-profile maker-checker (`GroupProfileRequestIT`); BDOI target-state profiles (`docs/modules/BDOI_TARGET_STATE_PROFILES.md`) |
| 1.4.4 | View or report of users and their roles for review | Met | Access matrix, certification extracts, user access reports (`UserAccessReportsIT`) |
| 1.5.1 | Audit of access attempts and of user and role changes | Met | `audit_log` (sign-ins, failures), `sec_access_change_log` (user and role changes) |
| 1.5.2 | Audit of start-up / shutdown, failures, configuration changes, audit-log access | Partly | Configuration changes audited (parameters, module switches, configuration promotion); start-up, shutdown and failures in the application log (CloudWatch). Access to the audit log screens is not itself logged |
| 1.5.3 | Audit of customer record access and updates, transactions, authentication changes | Partly | Updates and transactions audited per module; password and second-factor changes audited. Reads of client records are logged for the legacy inquiry only (`LEGACY_ACCESS_LOG_VIEW`) |
| 1.5.4 | APIs log failed and denied access and validation failures | Met | Integration request guard and gateway token checks logged; 401 / 403 counted in the metrics (`MetricsAccessIT`) |
| 1.5.5 | Storage access and administration logged | Partly | Document actions (upload, download links, legal hold, quarantine) audited in the application; S3 server access logging is set in the AWS account, outside this repository: to confirm |
| 1.5.6 | Log records time, user, source address, resource, outcome | Met | `audit_log` with `ip_address` and roles (V2700); real client address behind the load balancer (`SERVER_FORWARD_HEADERS_STRATEGY=native`) |
| 1.5.7 | No credentials, tokens or personal data in logs | Met | Request records mask `toString`; CR/LF neutralised (`logback-spring.xml`) |
| 1.5.8 | Logs exportable to remote monitoring | Met | JSON logs to stdout collected by CloudWatch; audit extracts (`docs/operations/RUNBOOK.md`) |
| 1.6.1 | Input validated against injection and overflow | Met | Bean Validation on request records; bound SQL parameters (`ArchitectureTest.sqlValuesAreBoundParameters`); React output encoding and CSP |
| 1.7.1 | Database with authentication, access control, audit | Met | Amazon RDS for PostgreSQL |
| 1.7.2 | Database and storage only after authentication | Met | Database credentials from the secret store; S3 through IAM role and presigned links |
| 1.7.3 | Public cloud storage protected by MFA | N/A | No storage object is public |
| 1.7.4 | Time-limited tokens for system transfers | Met | Presigned S3 links with short expiry (`DOCUMENT_STORAGE_DECISION.md`) |
| 1.7.5 | No hard-coded credentials | Met | Secrets from the environment (`CONFIGURATION.md`); `SeedPasswordGuardTest`, gitleaks in CI |
| 1.7.6 | Least privilege on the database | Partly | One application account owning the schema (Flyway migrations at start); a separate migration account is not yet used |
| 1.7.7 | No OS-command database functions | Met | None used (PostgreSQL, bound SQL) |
| 1.7.8 | Secure protocols to database and storage | Met | TLS verify-full to PostgreSQL outside dev; HTTPS to S3 |
| 1.7.9 | Sensitive data encrypted at rest (quantum-resistant algorithms) | Met | RDS and S3 encryption with KMS (AES-256); masked fields of migration data (`brokerverse.migration.masking-key`) |
| 1.7.10 | Critical data recoverable after ransomware | Partly | Daily backups and point-in-time recovery (`RUNBOOK.md` section 6); immutable or cross-account backup copies to be confirmed with BDOI IT |
| 1.8.1 | Password hashes with strong salted algorithms | Met | BCrypt (salted, cost 12) for test accounts; Entra ID in production |
| 1.8.2 | Authentication keys protected (HSM / secure store) | Met | Signing and encryption keys from AWS Secrets Manager / KMS (`CONFIGURATION.md`) |
| 1.8.3 | Secrets management solution in use | Met | AWS Secrets Manager (deployment) |
| 1.8.4 | No secrets in the application | Met | `ProductionSafeguards` refuses missing or default secrets; gitleaks |
| 1.8.5 | Secure protocols on every component, TLS 1.2+ | Met | TLS 1.2 / 1.3 (`deploy/nginx/default.conf.template`); Kafka SASL_SSL, Valkey TLS, PostgreSQL TLS outside dev |
| 1.8.6 | Quantum-resistant algorithms for network communications | Not met | TLS uses classical key exchange; hybrid ML-KEM depends on the load balancer and JDK support. To plan with BDOI IT |
| 1.9.1 | Errors without stack traces or server details | Met | `GlobalExceptionHandler`, `server.error.include-stacktrace: never`; `SecurityHeadersIT` |
| 1.9.2 | Source control with traceable, ticketed changes | Met | Git with reviewed merge requests and CI (`.gitlab-ci.yml`) |
| 1.9.3 | No unsupported or deprecated libraries and images | Partly | npm audit and Trivy in CI; OWASP dependency-check needs `NVD_API_KEY` in CI |
| 1.9.4 | Components from trusted repositories | Met | Maven Central, npm registry, official base images |
| 1.9.5 | Third-party libraries encapsulated | Partly | Integrations behind adapters (storage, identity, Kafka, mail); other libraries used directly |
| 1.9.6 | Least privilege for the service account | Met | Pods with `runAsNonRoot` and `readOnlyRootFilesystem` (`deploy/k8s/base`) |
| 1.9.7 | Critical application recoverable after ransomware | Partly | Stateless containers redeployed from the registry; see 1.7.10 |
| 1.10.1-1.10.9 | Payment card data requirements (9) | N/A | BIBS stores and processes no card data |

## ASR - 2 Web application

| Req. | Requirement (summary) | Status | Evidence / remark |
|---|---|---|---|
| 2.1.1 | CAPTCHA on public forms | N/A | No form is open without sign-in |
| 2.2.1 | Idle timeout (2 hours or less internal) | Met | `SESSION_TIMEOUT_MINUTES` = 30 with warning; access token 15 minutes |
| 2.2.2 | Random, unpredictable session IDs | Met | Signed JWT with random identifiers; random refresh tokens (`JwtTokenService`) |
| 2.2.3 | Session only after valid credentials | Met | `SignInTokensIT` |
| 2.2.4 | New session per sign-in; invalidated at logout | Met | Refresh token rotation and revocation list (`LogoutIT`) |
| 2.2.5 | Cookies HttpOnly and Secure | Met | Refresh cookie HttpOnly, Secure, SameSite=Strict (`RefreshCookies`) |
| 2.2.6 | Only server-issued session IDs accepted | Met | Signature, issuer and revocation checked on every request |
| 2.2.7 | Client-facing: one active session per user | N/A | Not client facing (sessions listed and ended on User Access) |
| 2.3.1 | No authorisation bypass through object references in URLs | Partly | Record ids appear in URLs, but every read and change is authorised by permission, data scope and object checks (`ObjectAccessIT`, `DataScopeIT`) |
| 2.4.1 | Server-side validation | Met | Bean Validation and service rules on every request |
| 2.4.2 | Upload name, extension, length and size checked | Met | `UploadRules` (extension list, 25 MB), `OfficeFileLimits` |
| 2.4.3 | Uploaded files scanned for malware | Met | Scan results and quarantine (`FILE_SCAN_RESULTS`, `FILE_QUARANTINE_VIEW`) |
| 2.4.4 | XML parsers protected against XXE | Met | Secure processing and no DTDs (`SamlResponseValidator`, `OdsTableReader`) |
| 2.4.5 | Sensitive data in body or headers, not query strings | Met | Credentials, tokens and personal data in request bodies; queries carry ids and filters |
| 2.4.6 | WAF and DDoS protection for public applications | N/A | Internal application, not exposed to the internet |
| 2.4.7 | Sensitive data handled by the back end only | Met | No OTP or contact routing through the browser |
| 2.4.8 | Framing prevented | Met | X-Frame-Options DENY, `frame-ancestors 'none'` (`SecurityHeadersIT`) |
| 2.4.9 | Content Security Policy | Met | CSP without inline script (`nginxSecurityHeaders.test.ts`) |
| 2.4.10 | No server version in headers | Met | nginx `server_tokens off`; no framework headers |
| 2.5.1 | Anti-caching headers | Met | Spring Security cache headers (`no-store`) on API answers; download links `no-store` (`FileLinkService`) |
| 2.5.2 | No sensitive data in client storage | Met | Tokens in memory / sessionStorage, never localStorage (`api/client.ts`); localStorage holds the remembered-device marker and display preferences only |
| 2.5.3 | Client data cleared at sign-out | Met | Session storage and in-memory state cleared at logout and timeout (`AuthProvider`) |

## ASR - 3 Web service / API

| Req. | Requirement (summary) | Status | Evidence / remark |
|---|---|---|---|
| 3.1.1 | Callers authenticated (OAuth2 public; credentials for internal) | Met | User API: bearer JWT; `/integration/**`: gateway OAuth2 tokens with scopes (`IntegrationSecurityConfig`) |
| 3.1.2 | Public APIs with mTLS or IP allow-listing | N/A | No public API; integrations through the BDO API gateway |
| 3.1.3 | Caller authorised per method | Met | `@PreAuthorize` per endpoint, per-API scopes (`IntegrationApiSecurityIT`) |
| 3.1.4 | WAF and DDoS for public APIs | N/A | No public API |
| 3.1.5 | Rate limiting | Met | Sign-in and integration rate limits (`LoginRateLimitFilter`, `IntegrationRequestGuard`) |
| 3.1.6 | Inactive tokens terminated | Met | Access token 15 minutes, refresh with rotation and idle timeout |
| 3.2.1 | API credentials and certificates not hard-coded | Met | Secret store and JWKS (`CONFIGURATION.md`) |
| 3.2.2 | TLS 1.2+ in transit | Met | See 1.8.5 |
| 3.2.3 | Sensitive data encrypted or masked | Met | Masking of personal data in extracts and migration staging; no authentication data in answers |
| 3.3.1 | Values, types and schema validated | Met | Request records with Bean Validation; JSON body limit (`JsonBodyLimitFilter`) |
| 3.3.2 | Only legitimate HTTP methods | Met | Method-specific mappings; 405 handled without detail (`SecurityHeadersIT`) |
| 3.3.3 | CSRF protection where cookies are used | Met | Refresh cookie needs `X-Requested-With`; bearer APIs carry no ambient credential |
| 3.3.4 | Generic error answers | Met | See 1.9.1 |
| 3.4.1 | Sensitive processing data kept in the back end | Met | See 2.4.7 |
| 3.4.2 | Answers limited to what the caller needs | Partly | Response records per screen; some lists (e.g. `/admin/users`) return more fields than every caller needs |

## ASR - 4 Batch

| Req. | Requirement (summary) | Status | Evidence / remark |
|---|---|---|---|
| 4.1.1 | Batch execution restricted to authorised users | Met | Jobs scheduled in the application (`JobScheduler`); manual runs need the job permissions |
| 4.1.2 | Extract targets strictly controlled | Met | Private S3 prefixes and the managed file transfer of BDO |
| 4.2.1 | Batch authenticates to the database | Met | Same authenticated datasource |
| 4.2.2 | Batch credentials not hard-coded | Met | Secret store |

## ASR - 5 Mobile and 6 Container

| Req. | Requirement (summary) | Status | Evidence / remark |
|---|---|---|---|
| 5.1.1-5.6.3 | Mobile application requirements (20) | N/A | No mobile application |
| 6.1.1 | Images and libraries from trusted registries | Met | Official base images, Trivy scan in CI |
| 6.1.2 | Containers least privilege (no root) | Met | `runAsNonRoot`, `readOnlyRootFilesystem` (`deploy/k8s/base`) |
| 6.2.1 | Secrets outside images, referenced at run time | Met | Environment from Kubernetes secrets / Secrets Manager |
| 6.2.2 | Secrets encrypted in storage | Met | Secrets Manager / KMS |

## Fraud-ASR

N/A for BIBS: the sheet applies to applications supporting financial and payment services under BSP
AFASA (fund transfers, withdrawals, merchant and bills payments, auto-debit, third-party enrolment).
BIBS is an internal broking and accounting system used by BDOI staff; customers do not sign in and
cannot initiate payments. Disbursements are prepared in BIBS and executed by the bank's payment
channels under their own controls.

| Req. | Requirement (summary) | Status |
|---|---|---|
| 1.1-1.7 | Transaction velocity, bot activity, risk-based limits, device and account-change monitoring, geolocation, unsafe merchants, behavioural anomalies (7) | N/A |
| 2.1-2.8 | Customer KYC at enrolment, 24-hour pause after key changes, unsafe devices, automation blocking, transaction integrity, device fingerprinting, strong customer authentication, no clickable links (8) | N/A |
| 3.1-3.5 | Real-time customer notifications and fraud alerts (5) | N/A |
| 4.1-4.5 | Payee verification, kill switch, third-party access management, money lock, customer limits (5) | N/A |
| 5.1-5.2 | Five-year retention and content of payment transaction logs (2)  | N/A (BIBS keeps its own audit and accounting records under the data retention rules, BRNB.106) |


## AI-ASR and AI-GOV (third-party assessment)

N/A: BIBS has no artificial-intelligence or generative-AI component, embeds no LLM and sends no data
to an AI service. The sheets apply when an application is an AI application ("An AI Application?" =
No on the ASR cover).

## Open items

| Item | Action |
|---|---|
| 1.2.5 / 1.2.14 minimum length fixed at 10 | Make the minimum length a business parameter (`PASSWORD_MIN_LENGTH`) so test accounts can follow 12 (administrators) or the 15 of the 2026 guidelines; production passwords are governed by Entra ID |
| 1.5.2 access to the audit log | Log the opening and extracts of the audit log screens |
| 1.7.6 database least privilege | Separate migration account; application account with DML only |
| 1.8.6 quantum-resistant TLS | Plan hybrid key exchange with BDO IT (load balancer, JDK) |
| 1.9.3 Java dependency scan | Provide `NVD_API_KEY` to CI |
