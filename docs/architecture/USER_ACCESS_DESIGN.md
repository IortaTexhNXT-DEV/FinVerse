# iNXT BrokerVerse - BDOI User Access Maintenance (BRD-11) Build Design

Status: **proposal for review**. This design extends [`BROKING_ARCHITECTURE.md`](BROKING_ARCHITECTURE.md) section 8 ("Broking administration", `nbadmin`), the Product Maintenance role-permission requests ([`PRODUCT_MAINTENANCE_DESIGN.md`](PRODUCT_MAINTENANCE_DESIGN.md) section 6.3) and the Developer Guide, which stay binding. It changes them only through the contract changes in section 9.

Requirements baseline: [`BDOI_UAM_BRD_SPEC.md`](../requirements/BDOI_UAM_BRD_SPEC.md). It has 160 functional ID lines (BRD 1.001.1 to BRD 4.003.1), 41 NFR rows (UAM-NFR-01 to 41) and questions UQ01-UQ19. Every class, migration and screen cites its BRD ID, for example `BRD 1.006.1` or `UAM-NFR-40`.

## 1. Design principles

1. **Extend, do not add a module.** User access already lives in `security` (users, roles, permissions, login, lockout) and `nbadmin` (access requests under four eyes, the User Access Matrix, retention). BRD-11 extends both. The request lifecycle, the approvers, bulk and the reports go to `nbadmin`. The user and role attributes, the change log, the sessions, the password policy and the directory seam go to `security`.
2. **A request is the only way to change access.** Users, their data and their group profiles (roles) change only through an approved request; the only exceptions are the bootstrap administrator and an audited emergency path (`UAM_DIRECT_ROLE_EDIT`, default off).
   - **User** requests are applied by the system on approval, or on their effective date (diagram p.6, "System to grant the access").
   - **Group-profile** requests are applied when the System Administrator implements the approved request (diagram p.6, "System Administrator to create / modify group profile"; UQ03 may simplify this to apply on approval).
3. **Four eyes, with a named approver.** The requester chooses the approver (BRD 1.00x "select approver from the drop-down"). The requester, and the user the request is about, can never decide it. Risky changes get a second approval (UAM-NFR-40).
4. **Structured, insert-only evidence.** Every applied change writes one row per attribute into `sec_access_change_log` (from / to, request number, done by). The row is insert-only, with a database trigger as in V26. The audit report (sample D) and the "added / modified / deactivated / reactivated by" columns (samples A-C) are read from this log. `audit_log` keeps its summaries.
5. **Directory authentication is a port.** BIBS must authenticate Windows IDs against BDO EUA / AD / LDAP (UAM-NFR-11, 17, 33; Q42). The interface is unknown, so `security` gets a `DirectoryAuthenticator` port with a LOCAL default. The lockout, audit and session behaviour stays identical in both modes.
6. **Backward compatible.** Existing request types, the endpoints of BROKING_ARCHITECTURE section 8, the PMADD05 role-permission requests and the demo data keep working. The new permissions are granted in the migration to every role that holds `ACCESS_REQUEST` today.
7. **No money.** User access posts no journal and publishes no accounting event.

## 2. Modules

| Module | Change | BRD | Depends on (unchanged) | Flyway (demo) |
|---|---|---|---|---|
| `security` (built) | `AppUser`: + Windows ID, business unit, user level, password dates, must-change flag, last logout. `Role`: + active, description, privilege level. `sec_access_change_log`, `sec_user_session`, `sec_password_history`. `AuthService`: directory port, logout, session log, password age. `UserAdminService`: change log, role (de)activation, password history, direct-edit guard. `effectivePermissions()` skips inactive roles. User ID pattern | 1.002.1.1.1, 1.003.1.1.2, 3.002.3-4, 4.002.x, 4.003.1; UAM-NFR-11-17, 33-37 | audit, system, common | V1060 (grants, parameters), V1061 (schema) |
| `nbadmin` (built) | Request lifecycle (DRAFT, RETURNED, CANCELLED, SCHEDULED, FOR_IMPLEMENTATION, IMPLEMENTED), types MODIFY_USER / CREATE_ROLE / DEACTIVATE_ROLE / REACTIVATE_ROLE, user type INTERNAL / EXTERNAL (portal users, section 4.4), chosen approver(s), effective date, history, bulk batches, risk rules and second approval, port `ExternalUserProvisioner`, `UAM_EFFECTIVE_CHANGES` job, four reports, screens | 1.002-1.009, 2.002, 3.002, 3.003, 4.003.1; UAM-NFR-14, 38-41 | security, system, approval, messaging, bulk, report, docgen, lov, alert | V1062 (V1960) |
| `system` (built) | Batch-failure e-mail to `JOB_FAILURE_RECIPIENTS` | UAM-NFR-25 | messaging | parameter in V1060 |

Sub-package ownership inside `nbadmin` (section 13): `nbadmin.access` (existing request classes move here only if the owner wishes; otherwise they stay in `nbadmin.domain` / `service` / `api`), `nbadmin.bulk` (handler), `nbadmin.report` (reports).

### 2.1 Dependency graph

```
nbadmin ──► security (UserAdminService, RoleRepository, AccessChangeLog, UserDirectory)
        ──► system, approval, messaging, bulk, report, docgen, lov, alert, audit
security ──► audit, system (parameters), common
(no new edges; security never depends on nbadmin)
```

## 3. Flyway allocation

Allocated range: **schema V1060-V1069, demo V1960-V1969**. Three schema versions are used and seven are kept free.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1060__uam_foundation.sql` | U0 | Roles `UAM_REQUESTOR`, `UAM_APPROVER`, `UAM_SECOND_APPROVER`. Grants of the new permissions (section 6), including every role that holds `ACCESS_REQUEST` today (BUSINESS_ADMIN, SYSADMIN), for compatibility. `sec_permission_action` rows for the new permissions (area `USER_ACCESS`), plus the missing area / action rows of existing permissions so the group-profile report can show a module for every task (BRD 3.003.2.2). Parameters (section 8). Updated description of `LOGIN_MAX_FAILED_ATTEMPTS` (CQ23 answered: all users). LOV types `UAM_BUSINESS_UNIT`, `UAM_USER_LEVEL`, `UAM_DEACTIVATION_REASON`. Alert codes, notification events |
| `V1061__security_user_access_extensions.sql` | U0 | `sec_user` + `windows_id` (unique, nullable), `business_unit_code`, `user_level`, `password_changed_at`, `must_change_password`, `last_logout_at`. `sec_role` + `active` (default true), `description`, `privilege_level` (default STANDARD; SYSADMIN = ADMIN). `sec_password_history`, `sec_user_session`, `sec_access_change_log` + insert-only trigger |
| `V1062__nbadmin_request_lifecycle.sql` | U1-A | `nba_access_request`: new columns (section 4.2, including the external-user columns of section 4.4); check constraints widened to the new statuses, types and user types. `nba_access_request_event`, `nba_access_request_approver`, `nba_access_request_batch`. `sec_access_change_log.subject_type` check widened to EXTERNAL_USER |
| `V1063-V1069` | - | Kept free (the EUA adapter configuration and a single-session rule if UQ04 / UQ09 require schema) |
| `db/demo/V1960__demo_user_access.sql` | U1-A | Users `requestor` (UAM_REQUESTOR), `uamapprover` (UAM_APPROVER), `secapprover` (UAM_SECOND_APPROVER). Requests in every status: draft, pending for `uamapprover`, returned, cancelled, scheduled (future date), approved and applied, a bulk batch of three lines, a CREATE_ROLE request FOR_IMPLEMENTATION for `admin`, a privileged change awaiting second approval. Change-log rows for the applied ones |

Rules:
- V1060 / V1061 run after V790 / V791 (`nba_access_request`), V755 (`sec_permission_action`) and V1000 (`LOGIN_MAX_FAILED_ATTEMPTS`) on a fresh database. V1062 runs after V1061.
- Foreign keys only to platform tables (`sec_user`, `sec_role`, `org_branch`, attachments).

## 4. Entities (key fields)

### 4.1 `security`

| Table / change | Key fields | Notes |
|---|---|---|
| `sec_user` + | `windows_id` varchar(50) unique, `business_unit_code` (LOV `UAM_BUSINESS_UNIT`), `user_level` (LOV `UAM_USER_LEVEL`), `password_changed_at`, `must_change_password`, `last_logout_at` | UAM-NFR-15. The status shown (ACTIVE, DISABLED, LOCKED, ONLINE) is derived from `enabled`, `locked` and an open session |
| `sec_role` + | `active` boolean, `description`, `privilege_level` (LOW, STANDARD, HIGH, ADMIN), `deactivated_at/by` | BRD 3.002.3-4, UAM-NFR-40. `AppUser.effectivePermissions()` and `UserDirectory.roleCodes()` skip inactive roles |
| `sec_access_change_log` | `occurred_at`, `subject_type` (USER, ROLE), `subject`, `activity` (CREATE_USER, MODIFY_USER, ROLES_CHANGED, DISABLE_USER, ENABLE_USER, UNLOCK, PASSWORD_RESET, CREATE_ROLE, ROLE_PERMISSIONS, DEACTIVATE_ROLE, REACTIVATE_ROLE), `attribute`, `from_value`, `to_value`, `request_no`, `done_by`, `approved_by` | Insert-only (trigger). Written by `UserAdminService` in the same transaction as the change |
| `sec_user_session` | `session_id` (JWT `jti`), `username`, `issued_at`, `last_seen_at` (throttled: at most once per 5 minutes), `ended_at`, `end_reason` (LOGOUT, IDLE_TIMEOUT, EXPIRED, ADMIN_ENDED, LOCKED) | UAM-NFR-35 (logout logging), status "online" (UQ13); prepares a single-session rule (UQ09) |
| `sec_password_history` | `user_id`, `password_hash`, `changed_at` | UAM-NFR-36; the last `PASSWORD_HISTORY_COUNT` hashes |

### 4.2 `nbadmin`

| Table / change | Key fields | Notes |
|---|---|---|
| `nba_access_request` + | `assigned_approver`, `effective_from` (date), `windows_id`, `business_unit_code`, `user_level`, `batch_id`, `role_name`, `role_description`, `privilege_level`, `submitted_by/at`, `cancel_reason`, `applied_at`, `implemented_by/at`, `second_approval_required`, `risk_flags` (CODE list: PRIVILEGE_INCREASE, OUTSIDE_HOURS); `user_type` (INTERNAL default, EXTERNAL), `party_kind` (INSURER / CLIENT), `party_code`, `portal_role` (section 4.4) | Status: DRAFT, PENDING, PENDING_SECOND, RETURNED, CANCELLED, REJECTED, APPROVED, SCHEDULED, FOR_IMPLEMENTATION, IMPLEMENTED (APPROVED = applied). Types: + MODIFY_USER, CREATE_ROLE, DEACTIVATE_ROLE, REACTIVATE_ROLE; CREATE_ROLE reuses `role_code` and `permissions_added` |
| `nba_access_request_event` | `request_id`, `action` (SAVE, SUBMIT, RETURN, RESUBMIT, CANCEL, APPROVE, SECOND_APPROVE, REJECT, SCHEDULE, APPLY, IMPLEMENT), `from_status`, `to_status`, `remarks`, `actor`, `occurred_at` | Insert-only history: every remark of the BRD ("add / save remarks") |
| `nba_access_request_approver` | `request_id`, `sequence`, `approver`, `decision` (PENDING, APPROVED, REJECTED, RETURNED), `remarks`, `decided_at` | Group-profile requests with several approvers (BRD 3.002.x "approver/s"); user requests have one row |
| `nba_access_request_batch` | `batch_no`, `attachment_id`, `bulk_job_id`, `lines`, `valid_lines`, `status` (mirrors the lines), `remarks` | BRD 1.009; each line is an `nba_access_request` with `batch_id` |

### 4.3 Request state machine (in `AccessRequest`, as today; no workflow case)

| From | Action | To | Who | Rule |
|---|---|---|---|---|
| (new) | save | DRAFT | requester (UAM_ENROLL / UAM_MODIFY / UAM_DEACTIVATE / UAM_REACTIVATE / UAM_GROUP_REQUEST by type) | Validation light (format only) |
| DRAFT | edit / cancel | DRAFT / CANCELLED | creator | Cancel is kept (never deleted) |
| DRAFT, RETURNED | submit / resubmit | PENDING (or PENDING_SECOND queue order) | creator | Full validation; one open request per user / role; approver(s) chosen and eligible; risk rules evaluated |
| PENDING | approve | APPROVED (applied now), SCHEDULED (future `effective_from`), FOR_IMPLEMENTATION (role types), PENDING_SECOND (risk flag), or the next approver in sequence | chosen approver (ACCESS_APPROVE), not the requester or the subject user | Temporary password shown once for CREATE_USER in LOCAL mode |
| PENDING_SECOND | approve | as above without the risk step | UAM_SECOND_APPROVER holder, not the first approver | UAM-NFR-40 |
| PENDING, PENDING_SECOND | return | RETURNED | approver | Remarks mandatory (BRD 2.002.7) |
| PENDING, PENDING_SECOND | reject | REJECTED | approver | Reason mandatory |
| PENDING, RETURNED | cancel | CANCELLED | creator (UAM_CANCEL) | Reason mandatory (BRD 1.007) |
| SCHEDULED | apply (job) | APPROVED | system `UAM_EFFECTIVE_CHANGES` | Re-validated on the date; failure -> alert and stays SCHEDULED |
| SCHEDULED | cancel | CANCELLED | creator or approver | UQ06 |
| FOR_IMPLEMENTATION | implement | IMPLEMENTED | System Administrator (ROLE_MANAGE) | Applies through `UserAdminService`; UQ03 |

### 4.4 External (portal) users (cross-BRD decision D7)

Portal users of the Employee Benefits design (insurer and client HR users, EMPLOYEE_BENEFITS_DESIGN section 6.3) are provisioned through these requests as an **external user type**. They are not `sec_user` rows: they live in `portal` (`ptl_user`) and sign in to the separate portal realm.

- Types CREATE_USER, DISABLE_USER and ENABLE_USER accept `user_type` = EXTERNAL with `party_kind`, `party_code` and `portal_role` (INSURER_USER, CLIENT_HR). MODIFY_USER and the role types stay INTERNAL only.
- Permissions: raising an EXTERNAL request needs `PORTAL_USER_REQUEST` (or `ACCESS_REQUEST`, section 6.1 compatibility); the chosen approver must hold `PORTAL_USER_APPROVE`. Both permissions are declared by the Employee Benefits design. The segregation rules of section 6.2 apply unchanged.
- The same lifecycle applies: draft, submit, return, cancel, approve, history (`nba_access_request_event`) and the change log (`sec_access_change_log`, subject type EXTERNAL_USER).
- On approval, `AccessChangeApplier` calls the port `nbadmin.service.ExternalUserProvisioner` (`create`, `disable`, `enable`), which `portal` implements (`PortalUserProvisioner`: creates `ptl_user` INVITED and sends the invitation link; no password is sent). Without a `portal` module the default adapter refuses EXTERNAL requests with `EXTERNAL_USERS_NOT_AVAILABLE`. `nbadmin` never depends on `portal`.
- Lock / unlock and the portal login and download logs stay in `portal` (`PORTAL_ADMIN`). The lockout after 3 failed attempts applies to portal users as well (`PORTAL_MAX_FAILED_LOGINS` = 3; CQ23 answered: all users).

## 5. Accounting events and GL entries

None.

## 6. Security

### 6.1 Permissions (added to `security.domain.Permission` in U0)

| Permission | Function (BRD 4.002.2.x) | Area / action class |
|---|---|---|
| `UAM_ENROLL` | 1. Enroll new user | USER_ACCESS / CREATE |
| `UAM_MODIFY` | 2. Modify existing user | USER_ACCESS / AMEND |
| `UAM_DEACTIVATE` | 3. Deactivate user | USER_ACCESS / AMEND |
| `UAM_REACTIVATE` | 4. Reactivate user | USER_ACCESS / AMEND |
| `UAM_CORRECT` | 5. Apply correction (edit a returned request) | USER_ACCESS / AMEND |
| `UAM_CANCEL` | 6. Cancel a request | USER_ACCESS / AMEND |
| `UAM_VIEW` | 7. View requests (own; all with ACCESS_APPROVE / USER_MANAGE / AUDIT_VIEW) | USER_ACCESS / VIEW |
| `ACCESS_APPROVE` (exists) | 8. Review and approve requests | USER_ACCESS / APPROVE (re-classified from BROKING_ADMIN) |
| `UAM_GROUP_REQUEST` | 9. Submit group-profile requests | USER_ACCESS / CREATE |
| `UAM_REPORT_VIEW` | 10. Generate reports | USER_ACCESS / VIEW |
| `UAM_SECOND_APPROVE` | Second approval of privileged / out-of-hours changes (UAM-NFR-40) | USER_ACCESS / APPROVE |
| `ACCESS_REQUEST` (exists) | Kept as the umbrella of the request functions for existing roles and endpoints | USER_ACCESS / CREATE |

The request endpoints check the type-specific permission, **or** `ACCESS_REQUEST` (compatibility).

### 6.2 Roles (V1060) and demo users (V1960, password `Brokerverse@2026`)

| Role | Persona | Permissions | Demo user |
|---|---|---|---|
| `UAM_REQUESTOR` (new) | Requestor | UAM_ENROLL, UAM_MODIFY, UAM_DEACTIVATE, UAM_REACTIVATE, UAM_CORRECT, UAM_CANCEL, UAM_VIEW | `requestor` |
| `UAM_APPROVER` (new) | Approver | ACCESS_APPROVE, UAM_VIEW, UAM_REPORT_VIEW | `uamapprover` |
| `UAM_SECOND_APPROVER` (new) | Additional reviewer of risky changes (UQ07) | UAM_SECOND_APPROVE, UAM_VIEW | `secapprover` |
| `BUSINESS_ADMIN` (exists) | Business Administrator | + UAM_GROUP_REQUEST, UAM_VIEW, UAM_REPORT_VIEW, UAM_CORRECT, UAM_CANCEL (keeps ACCESS_REQUEST) | `badmin` |
| `NB_APPROVER` (exists) | BRD-1 approver | keeps ACCESS_APPROVE until BDOI's matrix (OQ48) | `approver` |
| `SYSADMIN` (exists) | System Administrator | + UAM_VIEW, UAM_REPORT_VIEW; ROLE_MANAGE now implements approved group-profile requests | `admin` |
| `AUDITOR` (exists) | Auditor | + UAM_VIEW, UAM_REPORT_VIEW | `auditor` |

Segregation rules, enforced in `AccessRequestService`:
- the requester and the subject user never decide the request;
- the second approver differs from the first approver;
- the implementer of a group-profile request is not its requester.

## 7. Workflows

No `workflow` case. The request keeps its own state machine (section 4.3), as the BRD-1 access requests do. Pending items appear in **My Approvals** through `AccessRequestApprovalSource`:
- for the chosen approver, or for every approver when `UAM_ANY_APPROVER` = true;
- PENDING_SECOND for holders of UAM_SECOND_APPROVE;
- FOR_IMPLEMENTATION for ROLE_MANAGE.

## 8. Jobs, parameters, LOVs, alerts, notifications

| Job | Cron property (default) | Work |
|---|---|---|
| `UAM_EFFECTIVE_CHANGES` | `brokerverse.jobs.uam-effective-changes-cron` (`0 5 0 * * *`) | Apply SCHEDULED requests whose `effective_from` is today or earlier (UAM-NFR-14) |
| `PASSWORD_EXPIRY_NOTICE` | `brokerverse.jobs.password-expiry-notice-cron` (`0 0 6 * * *`) | LOCAL mode only: notify users whose password expires within 7 days |

| Parameter (`sys_parameter`, category SECURITY) | Default | Source |
|---|---|---|
| `AUTH_MODE` | LOCAL (LOCAL, DIRECTORY) | UAM-NFR-11 / 17; DIRECTORY once the EUA adapter exists |
| `USER_ID_PATTERN` | `^[a-zA-Z][0-9]{9}$` (to confirm) | UAM-NFR-13, UQ05 |
| `PASSWORD_HISTORY_COUNT` | 8 | CLXN NFR (UQ08) |
| `PASSWORD_MAX_AGE_DAYS` | 90 | CLXN NFR (UQ08) |
| `PASSWORD_MIN_AGE_DAYS` | 1 | CLXN NFR (UQ08) |
| `UAM_WORKING_HOURS` | `08:00-18:00,MON-FRI` (to confirm) | UAM-NFR-40, UQ07 |
| `UAM_ANY_APPROVER` | false | UQ02 |
| `UAM_DIRECT_ROLE_EDIT` | false | PQ17 emergency path |
| `UAM_ROLE_APPLY_ON_APPROVAL` | false (the p.6 diagram: System Administrator implements) | UQ03 |
| `JOB_FAILURE_RECIPIENTS` | empty (e-mail list) | UAM-NFR-25 |
| `LOGIN_MAX_FAILED_ATTEMPTS` (exists) | 3, all users | UAM-NFR-19, CQ23 |

LOVs: `UAM_BUSINESS_UNIT`, `UAM_USER_LEVEL` (both empty until UQ05), `UAM_DEACTIVATION_REASON` (RESIGNED, TRANSFERRED, LONG_LEAVE, SECURITY, OTHERS; to confirm).

Alert codes (module USER_ACCESS):
- `UAM_PRIVILEGED_CHANGE` (a request with a risk flag);
- `UAM_SCHEDULED_APPLY_FAILED`;
- `UAM_DIRECT_ROLE_EDIT` (an emergency edit was used).

Notification events:
- `UAM_REQUEST_TO_APPROVE`;
- `UAM_REQUEST_RETURNED`;
- `UAM_REQUEST_CANCELLED`;
- `UAM_REQUEST_DECIDED` (requester);
- `UAM_ACCESS_CHANGED` (affected user, in-app and e-mail);
- `UAM_SECOND_APPROVAL`;
- `UAM_FOR_IMPLEMENTATION`.

## 9. Impact on built modules (contract changes)

| Module | Change | Contract | Owner / wave |
|---|---|---|---|
| `security` domain | `AppUser` fields and getters; `Role.active` / `privilegeLevel` / `description`; `effectivePermissions()` skips inactive roles | New columns only; no signature removed. **Behaviour change:** a deactivated role grants nothing. No role is inactive until a DEACTIVATE_ROLE request is approved | U0 |
| `security` `UserAdminService` | Writes `sec_access_change_log` in `createUser`, `updateUser`, `unlock`, `resetPassword`, `createRole`, `updateRole`; new `deactivateRole` / `reactivateRole`; `updateUser` accepts the new attributes through an extended `UserRequest` (new optional fields, defaults keep current callers valid); password history and age on `changeOwnPassword` / `resetPassword`; `must_change_password` after an admin reset | Additive; `UserRequest` gets optional components `windowsId`, `businessUnitCode`, `userLevel` (a secondary constructor keeps the current 7-argument form) | U0 |
| `security` `UserAdminController` | `POST /admin/roles` and `PUT /admin/roles/{id}` refused with `ROLE_EDIT_BY_REQUEST` unless `UAM_DIRECT_ROLE_EDIT` = true or the call carries an approved FOR_IMPLEMENTATION request number | PQ17 answered. **Behaviour change** for SYSADMIN; the Roles screen shows "Implement request" instead of free editing | U0 (guard) + U1-A (implementation flow) |
| `security` `AuthService` / `AuthController` | `DirectoryAuthenticator` port (default `LocalPasswordAuthenticator`); `POST /auth/logout` (ends the session, audits LOGOUT); session rows on login; `AuditAction.LOGOUT` added; `must_change_password` flag in `LoginResponse`; `GET /auth/password-reset` flow (LOCAL mode) | Additive; the login contract is unchanged in LOCAL mode | U1-B |
| `security` `JwtTokenService` | Adds a `jti` claim; `JwtAuthenticationFilter` refuses a token whose session is ended | Tokens issued before the release have no `jti` and are accepted until they expire | U1-B |
| `audit` | `AuditAction.LOGOUT` | Additive enum constant | U0 |
| `nbadmin` | Everything in sections 4.2-4.3; `AccessRequestRequest` / `AccessRequestResponse` get the new optional fields; new endpoints: `PUT access-requests/{id}` (edit draft), `POST access-requests/{id}/submit | return | cancel | implement | second-approve`, `GET access-requests/{id}/history`, `GET approvers?type=`, `POST access-requests/bulk` (multipart), reports | The existing `POST access-requests` still creates and submits in one call (compatibility for PMADD05 screens); a new flag `draft=true` saves a draft | U1-A |
| `nbadmin` `AccessChangeApplier` | New types; passes the request number to `UserAdminService` for the change log | Internal | U1-A |
| `system` | JOB_FAILURE also e-mails `JOB_FAILURE_RECIPIENTS` | Additive | U1-B |
| frontend `features/admin` | Users page: new columns (Windows ID, business unit, user level, status incl. online), filter by status; direct create / edit of users limited to SYSADMIN with `UAM_DIRECT_ROLE_EDIT` (otherwise "Raise request"); Roles page: active flag, privilege level, "Implement request" | Screen changes | U1-A |
| frontend `features/profile` | Edit own e-mail / mobile (UQ17); password policy messages (history, age) | Screen changes | U1-B |
| frontend sign-in | "Forgot password?" (LOCAL mode), EUA message pass-through (DIRECTORY mode), forced password change | Screen changes | U1-B |
| Collections (being built) | CQ23 answered: lockout 3 is global (already so); **no single-role check** should be built; single session per device remains open (UQ09) | Remove the single-role policy check from the Collections plan (COLLECTIONS_DESIGN section 6.2 note) | Collections owner |
| Employee Benefits (`portal`, designed) | Decision D7: portal users are provisioned through these requests as user type EXTERNAL (section 4.4); EB drops its own `ptl_user_request` | `portal` implements `ExternalUserProvisioner`; the port is committed by U1-A before EB wave E1-A | U1-A (port and type), EB E1-A (adapter) |
| Product Maintenance (`productmaint`, V791 requests) | PQ17 answered: role-permission changes stay requests; after approval they now wait for SYSADMIN implementation (FOR_IMPLEMENTATION) unless UQ03 decides "apply on approval" | Behaviour switch `UAM_ROLE_APPLY_ON_APPROVAL` (default false per the BRD diagram) | U1-A |
| Operations, Collections, Accounting, GL, NB business modules | None: permissions and roles are unchanged; only the way roles change is governed | - | - |

## 10. Integrations to park (seam only)

| Item | Question | Seam |
|---|---|---|
| BDO EUA with Windows ID | Q42, UQ04 | `security.service.DirectoryAuthenticator` (`DirectoryResult authenticate(String windowsId, char[] password)` returning SUCCESS / INVALID / LOCKED / ERROR with the EUA message). `AUTH_MODE` = DIRECTORY maps the user by `windows_id`. The lockout counter and the audit are unchanged; the password is never stored in DIRECTORY mode |
| LDAP / Active Directory | UQ04 | Same port; an adapter on Spring Security LDAP (bind authentication) when BDO gives the host, base DN and TLS certificate |
| SSO (SAML / OIDC) | UQ04 | Same port family: `SsoTokenExchange` for an IdP assertion -> BIBS JWT; not built |
| External ACL | UQ14 | `security.service.ExternalAuthorization` (default: none) |
| Remote log shipping / syslog | UAM-NFR-21 | Platform logging configuration (Logback appender), infrastructure |
| HR feed (joiners / leavers) | not in the BRD | None; bulk requests cover mass changes |

## 11. Reports and screens

### 11.1 Reports (`nbadmin.report`, category Control & Audit, view `UAM_REPORT_VIEW`)

| Code | BRD | Content | Parameters |
|---|---|---|---|
| `UAM-USER-ACCESS` | 3.003.1, sample A | User name, user ID, Windows ID, group profile(s), business unit, level, status, created by / date, modified by / date, last action and actor | as of date, business unit, status, group profile |
| `UAM-GROUP-PROFILE` | 3.003.2, sample B | Group profile, module (area), task (permission / action class), With / No Access; created / modified dates and actors | group profile, area, active |
| `UAM-GROUP-MEMBERS` | 3.003.3, sample C | Group profile, member user name and ID, added by / date, modified by / date | group profile, as of date |
| `UAM-AUDIT-LOG` | 4.003.1, sample D | Date, activity, from, to, done by, approved by, request no.; logins, failed logins and logouts optional | date from / to, user, activity |
| `UAM-REQUESTS` | 1.008 | Requests by status, type, requester, approver, age | date range, status, type |

The existing `CTL-AUDIT` and the matrix export stay.

### 11.2 Screens

The navigation follows `docs/design/BDO_UX_GUIDELINES.md` section 3. Today, Access Requests and User Access Matrix sit in the Broking Setup section. They move to a new section **"User Access"** in the group **Setup & Administration**, before Administration (`features/nbadmin/userAccessModule.ts`, id `user-access`, home `/user-access/requests`):

| Screen | Route | Permission | BRD |
|---|---|---|---|
| Access Requests | `/user-access/requests` (old `/broking-setup/access-requests` redirects) | UAM_VIEW / ACCESS_REQUEST / ACCESS_APPROVE (`alsoPermissions`) | Tabs: My Requests, Assigned to Me, Second Approval, For Implementation, All. Work-list toolbar, filters (status, type, requester, approver, date); detail page with History (1.008, 2.002) |
| New / Edit Request | `/user-access/requests/new`, `/user-access/requests/:id/edit` | type permission | One form for the four user types: user search, user data, group profiles, effective date, approver, remarks; Save Draft / Submit / Cancel (1.002-1.007). User type External (portal users, section 4.4) shows party and portal role instead of group profiles |
| Bulk Request | `/user-access/bulk` | UAM_ENROLL or UAM_MODIFY | Template download, upload, row validation report, draft batch, submit (1.009) |
| Group Profile Requests | `/user-access/group-profiles` | UAM_GROUP_REQUEST / ROLE_MANAGE | Create / modify / deactivate / reactivate with a permission picker by area and action (the existing `RolePermissionFields`), approver list, Implement (SYSADMIN) (3.002) |
| User Access Matrix | `/user-access/matrix` (moved) | as today | BRD-3 PMADD05 / BRD 3.3.4 |
| User Access Reports | `/user-access/reports` | UAM_REPORT_VIEW | Links to the five reports (3.003, 4.003) |

Administration > Users and Roles stay for the System Administrator (implementation, unlock, reset). Help entries go in `features/nbadmin/help.ts` (routes changed) and are registered in `HELP_SECTIONS`.

## 12. NFR design notes

- **Performance.** Fewer than 30 users; every screen is a paged query; the reports are SQL over `sec_*` / `nba_*` tables (UAM-NFR-01-02).
- **Retention.** The request history, the access-change log and the session log are kept at least as long as `audit_log` (no purge). The QPS policy values are UQ12. Add retention rules `ACCESS_REQUEST` (CANCELLED, REJECTED) with action REVIEW only.
- **Security.** Temporary passwords are shown once and never stored in clear (as today). Password reset links are single-use and expire in 30 minutes. No secrets go in `sys_parameter`.

## 13. Build-wave plan

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **U0** (1 agent, short; **the same foundation agent as S0 of BRD-10**, or run after it) | User access foundation | Permissions, `AuditAction.LOGOUT`, security domain extensions, change log writes, role activation and effective permissions, direct role-edit guard, V1060, V1061, nav section registration (stub), crons, Developer Guide range rows | `security/domain/**`, `security/service/UserAdminService.java`, `security/service/UserDirectory.java`, `security/api/UserAdminController.java`, `security/api/dto/UserRequest.java`, `audit/domain/AuditAction.java`, `V1060`, `V1061`, `navigation/modules.ts`, `help/helpContent.ts`, `features/nbadmin/userAccessModule.ts`, `application.yml`, `docs/operations/CONFIGURATION.md` | `mvn verify` green; existing security and nbadmin tests unchanged, plus tests for inactive roles and the change log |
| **U1-A** | Request lifecycle and screens | `nbadmin` lifecycle, types, user type EXTERNAL and the `ExternalUserProvisioner` port with its refusing default (committed first, for EB E1-A), approvers, second approval, effective-date job, bulk handler, implementation flow, approval source, frontend User Access screens, admin Users / Roles screen changes, demo V1960 | `nbadmin/domain/**`, `nbadmin/service/Access*`, `nbadmin/service/bulk/**`, `nbadmin/api/Access*`, `V1062`, `db/demo/V1960`, `features/nbadmin/**` (except reports), `features/admin/UsersPage.tsx`, `RolesPage.tsx` | Draft -> submit -> return -> correct -> approve -> applied; scheduled -> applied by the job; group profile -> FOR_IMPLEMENTATION -> implemented; bulk batch of 3 |
| **U1-B** | Authentication, passwords, sessions, reports | Directory port and LOCAL adapter, logout, session log, `jti`, password history / age / forced change / self-service reset, own profile edit, batch-failure e-mail, the five reports | `security/service/Auth*`, `JwtTokenService.java`, `JwtAuthenticationFilter.java`, `security/service/directory/**`, `security/api/AuthController.java`, `system/service/JobFailureListener.java`, `nbadmin/report/**`, `features/profile/**`, `auth/**`, the sign-in page | Logout audited; the audit report shows from / to; password rules enforced; sessions listed |
| **U2** | Hardening | E2E, `ApiSmokeIT` entries, update of `BROKING_ARCHITECTURE.md` section 8 (by its owner), fit/gap refresh | tests + docs | Full `mvn verify` / `npm run verify` |

Rules for parallel work:
- One Flyway file set per agent: U0 V1060 / V1061; U1-A V1062 / V1960.
- `security/domain/**` belongs to U0. After U0 merges, U1-B changes only the security service / api files listed for it.
- U1-A and U1-B both read `UserAdminService`; any change to it after U0 goes through U1-A.
- Shared files (the Permission enum, nav, help registry, `application.yml`) are edited **only in U0 / S0**.
- BRD-10 (Sanction Screening) S0 edits the same shared files, so S0 and U0 are **one agent** or run one after the other.

## 14. What depends on information BDOI has not given

| Item | Question | Built now | Parked |
|---|---|---|---|
| EUA / AD / SSO | Q42, UQ04 | Port, LOCAL mode, `windows_id` field | Adapter |
| Approver population and rules | UQ01, UQ02 | Named approver, eligibility by permission, `UAM_ANY_APPROVER` | Per-unit approver lists |
| Implementation step for group profiles | UQ03 | FOR_IMPLEMENTATION step; `UAM_ROLE_APPLY_ON_APPROVAL` | - |
| Risky-change rules | UQ07 | Privilege levels, working-hours parameter, second approval | Final levels per role |
| Password values | UQ08 | Parameters seeded with the CLXN values | - |
| Single session per device | UQ09 | Session log | Enforcement |
| Business unit / user level values | UQ05 | LOVs (empty) | Values |

## 15. Risks

1. **Behaviour change for administrators.** SYSADMIN can no longer edit roles directly. Mitigation: the emergency parameter (audited, alert) and a clear "Implement request" flow; announce it in the release notes.
2. **Directory authentication unknown.** Mitigation: the port and the LOCAL mode; the Windows ID is captured now, so the switch is a configuration change.
3. **Shared security files.** The two BRD foundations and the Collections / Accounting build agents touch `Permission.java`. Mitigation: one foundation agent, additive changes only, merge early.
4. **Inactive roles.** A deactivated role could remove rights from active users by surprise. Mitigation: the request shows the members affected, and UQ16 decides whether deactivation is blocked while the role has members.
