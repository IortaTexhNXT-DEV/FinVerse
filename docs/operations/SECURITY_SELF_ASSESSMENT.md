# Security self-assessment (OWASP Top 10 2021, OWASP API Top 10 2023)

Prepared before the third-party penetration and performance tests, with User Access and the
`/integration` APIs in focus. Status: **Met** (control in place and tested), **Config** (in place,
depends on the deployment values), **Open** (known gap, owner action). Paths are relative to the
repository root; `IT`/`Test` names are under `backend/src/test/java`.

## Access control and identity

| Control | Status | Evidence |
|---|---|---|
| Every user-API endpoint declares who may call it (`@PreAuthorize` on the method or class; public ones `permitAll()`) | Met | `ArchitectureTest.endpointsDeclareWhoMayCall`; `PermissionConsistencyTest` (each permission granted by a migration, each UI permission checked on the server) |
| Deny by default for unmatched URLs; `/integration/**` only with gateway tokens and per-API scopes | Met | `SecurityConfig` (`anyRequest().authenticated()`), `IntegrationSecurityConfig` (`denyAll` for paths without a rule); `IntegrationApiSecurityIT` |
| Object-level access (IDOR) on user, session and group-profile ids | Met | `ObjectAccessIT`; requests of others hidden (`AccessRequestVisibility`, `AccessRequestIT`); data scope per company (`ArchitectureTest.companyEndpointsAreDataScoped`, `DataScopeIT`) |
| Read-only auditor persona | Met | `UAM_AUDITOR` (V2831): `ObjectAccessIT.theAuditorReadsTheUsersButChangesNothing`, `PersonaMenusIT` |
| No user creation or change outside UIDM-ISC; no break-glass; local passwords only for test accounts outside production | Met | `UserAdminApiIT.usersAreNotCreatedNorChangedThroughTheApi`, `PasswordResetRules`, `LocalPasswordSignInOffIT`, `ProductionSafeguards` |
| Mass assignment: request bodies are request records, never entities | Met | `ArchitectureTest.requestBodiesAreNotEntities`; SCIM input read field by field (`ScimUsers.read`, `ScimInput`) |
| Separation of duties: maker never approves or implements; SoD rules on group profiles | Met | `GroupProfileRequestIT`, `PermissionCombinationIT`, `UamSignoffControlsIT` |

## Authentication and session

| Control | Status | Evidence |
|---|---|---|
| Sign-in through Microsoft Entra ID (OIDC, PKCE, nonce, state); second factor at Entra ID | Config | `SingleSignOnIT`; `docs/operations/CONFIGURATION.md` "Authentication and session security" |
| JWT: HMAC key of at least 256 bits, issuer and `use` checked, short access token, revocation list, refresh token rotation with reuse detection | Met | `JwtTokenService`, `SignInTokensIT`, `LogoutIT`, `SessionLogIT` |
| Gateway tokens of `/integration`: asymmetric algorithms only, issuer, audience, expiry, JWKS timeout; unconfigured = refuse all | Met | `IntegrationTokenDecoder`, `IntegrationApiSecurityIT` |
| Refresh cookie HttpOnly, SameSite=Strict, Secure (outside a developer machine), path `/api/v1/auth` | Met | `RefreshCookies`, `SignInTokensIT` |
| CSRF: bearer-token APIs exempt (no ambient credential); the cookie refresh needs `X-Requested-With` | Met | `SecurityConfig`, `SignInTokensIT` |
| Rate limit on sign-in (20 per address and minute) and password reset; lockout after `LOGIN_MAX_FAILED_ATTEMPTS` | Met | `LoginRateLimitFilter`, `LoginProtectionTest`, `SignInPasswordIT`; real client address behind the load balancer: `SERVER_FORWARD_HEADERS_STRATEGY=native` (`deploy/k8s/base/backend-config.yaml`) |
| Uniform answer and timing for unknown, locked, deactivated user and wrong password | Met | `AuthService.check` (an unknown user costs a password check), `LocalPasswordAuthenticator.INVALID` |
| Password checks cannot starve the server (BCrypt outside the transaction, one check per processor) | Met | `AuthService`, `LocalPasswordAuthenticator`; `perf/uam/README.md` (`mixed.js`) |
| Integration rate limit and body size (300 requests a minute per address, 256 KB) | Config | `IntegrationRequestGuard`; a UIDM-ISC full re-sync (about 2,600 calls) takes about 9 minutes at 300 a minute: agree the value (`BROKERVERSE_INTEGRATION_RATE_LIMIT`) with BDOI IT |

## Input, output and errors

| Control | Status | Evidence |
|---|---|---|
| SQL injection: bound parameters only | Met | `ArchitectureTest.sqlValuesAreBoundParameters` |
| XSS: React output encoding, CSP without inline script or `eval` | Met | `frontend/nginx.conf`, `deploy/nginx/default.conf.template`, `frontend/src/test/nginxSecurityHeaders.test.ts` |
| CSV / spreadsheet formula injection in extracts | Met | `CertificationExtract.appendCsv` (leading `= + - @` quoted); XLSX cells written as text |
| Errors without stack trace or server detail (500 with a reference only; 405 and 415 handled) | Met | `GlobalExceptionHandler`, `server.error.include-stacktrace: never`; `SecurityHeadersIT.wrongMethodsTypesAndPagesAreAnsweredWithoutAStackTrace` |
| Unbounded lists: page size at most 500 on `/api/**`; SCIM pages at most 200 | Met | `PageSizeLimitFilter` (`PageSizeLimitFilterTest`), `ScimAnswers.Page` (`IdentityProvisioningIT`) |
| JSON body limit 2 MB; uploads 25 MB with type, text and decompression checks | Met | `JsonBodyLimitFilter`, `spring.servlet.multipart`, `OfficeFileLimits`; CONFIGURATION.md "Request size" |
| Log forging and secrets in logs: CR/LF neutralised; no password, token or secret logged | Met | `logback-spring.xml`; review of `LOG.*` calls (only lock fencing numbers and user names) |

## Transport, headers and configuration

| Control | Status | Evidence |
|---|---|---|
| HSTS (1 year, sub-domains), X-Content-Type-Options, X-Frame-Options DENY, `frame-ancestors 'none'`, Referrer-Policy no-referrer, Permissions-Policy | Met | Backend `SecurityConfig` (`SecurityHeadersIT`); nginx (`nginxSecurityHeaders.test.ts`) |
| CORS limited to `BROKERVERSE_ALLOWED_ORIGINS`; another origin gets no grant | Met | `SecurityConfig.corsSource`, `SecurityHeadersIT.anOriginNotAllowedGetsNoCorsGrant` |
| TLS 1.2/1.3 with forward-secret ciphers on the frontend; TLS to PostgreSQL verify-full | Met | `deploy/nginx/default.conf.template`, `application.yml` |
| API documentation, simulators and seed data off in production | Met | `ProductionSafeguards` (`ProductionSafeguardsTest`) |
| Actuator: health and info only on the application port | Met | `MetricsAccessIT` |

## Supply chain and monitoring

| Control | Status | Evidence |
|---|---|---|
| npm dependencies: `npm audit` (high and critical fail) | Met | `.gitlab-ci.yml`; run 16-Oct-2026: 0 vulnerabilities |
| Java dependencies: OWASP dependency-check, CVSS >= 7 fails, suppressions with reason and expiry | Config | `backend/pom.xml`, `backend/quality/dependency-check-suppressions.xml`; needs `NVD_API_KEY` in CI |
| Secrets scanning and container scan | Met | `.gitleaksignore`, `.trivyignore.yaml`, CI jobs |
| Audit trail of sign-ins, failures, access changes, extracts (with SHA-256) | Met | `audit_log`, `sec_access_change_log`, `CertificationExtract`; `UserAccessReportsIT` |

## Open items

| Item | Owner action |
|---|---|
| A dormant user deactivated here comes back active if UIDM-ISC sends the account as active | UIDM-ISC disables the account on the notice "Dormant users deactivated" |
| `/admin/users` returns every user (425 KB for 1,200 users) | Server paging above a few thousand users |
| Two sign-ins of the same account at the same instant can answer 409 | Accept (test accounts only), or retry once on the optimistic lock |
| Integration rate limit vs. a full re-sync (see above) | Agree the value with BDOI IT before the load test |
