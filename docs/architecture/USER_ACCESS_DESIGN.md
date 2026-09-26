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
5. **Directory authentication is a port.** BIBS must authenticate Windows IDs against BDO EUA / AD / LDAP (UAM-NFR-11, 17, 33; Q42). The interface is unknown, so `security` gets a `DirectoryAuthenticator` port with a LOCAL default. The lockout, audit and session behaviour stays identical in both modes. **Update 26-Sep-2026:** BDOI named the identity integrations of Drop 0 (`PROGRAMME_ALIGNMENT.md` section 5): sign-in goes through **EIAM on Microsoft Entra ID** (OpenID Connect redirect, not a password bind) and user provisioning may come from **UIDM-ISC**, BDO's identity governance (IGA) tool. Section 10.1 gives the design; both are open with BDOI IT (IQ04, IQ05; register DCR-229, DCR-230).
6. **Backward compatible.** Existing request types, the endpoints of BROKING_ARCHITECTURE section 8, the PMADD05 role-permission requests and the seed data keep working. The new permissions are granted in the migration to every role that holds `ACCESS_REQUEST` today.
7. **No money.** User access posts no journal and publishes no accounting event.

## 2. Modules

| Module | Change | BRD | Depends on (unchanged) | Flyway (seed) |
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

Allocated range: **schema V1060-V1069, seed V1960-V1969**. Five schema versions are used and five are kept free.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1060__uam_foundation.sql` | U0 | Roles `UAM_REQUESTOR`, `UAM_APPROVER`, `UAM_SECOND_APPROVER`. Grants of the new permissions (section 6), including every role that holds `ACCESS_REQUEST` today (BUSINESS_ADMIN, SYSADMIN), for compatibility. `sec_permission_action` rows for the new permissions (area `USER_ACCESS`), plus the missing area / action rows of existing permissions so the group-profile report can show a module for every task (BRD 3.003.2.2). Parameters (section 8). Updated description of `LOGIN_MAX_FAILED_ATTEMPTS` (CQ23 answered: all users). LOV types `UAM_BUSINESS_UNIT`, `UAM_USER_LEVEL`, `UAM_DEACTIVATION_REASON`. Alert codes, notification events |
| `V1061__security_user_access_extensions.sql` | U0 | `sec_user` + `windows_id` (unique, nullable), `business_unit_code`, `user_level`, `password_changed_at`, `must_change_password`, `last_logout_at`. `sec_role` + `active` (default true), `description`, `privilege_level` (default STANDARD; SYSADMIN = ADMIN). `sec_password_history`, `sec_user_session`, `sec_access_change_log` + insert-only trigger |
| `V1062__nbadmin_request_lifecycle.sql` | U1-A | `nba_access_request`: new columns (section 4.2, including the external-user columns of section 4.4); check constraints widened to the new statuses, types and user types. `nba_access_request_event`, `nba_access_request_approver`, `nba_access_request_batch`. `sec_access_change_log.subject_type` check widened to EXTERNAL_USER |
| `V1063__security_sign_in_and_passwords.sql` | U1-B | Sign-in, passwords, sessions and the user access reports (see the migration header) |
| `V1064__hide_insurer_roles_and_permissions.sql` | Insurer suite hiding | Withdraws the insurer-only permissions (`Permission.isInsurerOnly`: `POLICY_*`, `CLAIM_*`, `REINSURANCE_*`, `RESERVE_*`, `CONSOLIDATION_RUN`, `INSURER_TAX_VIEW`) from every role and deactivates `UNDERWRITER`, `CLAIMS_OFFICER`, `RI_OFFICER`, with access change log rows. The User Access screens neither list nor accept these permissions or the roles holding them ([`CODEBASE_RELEVANCE_AUDIT.md`](../development/CODEBASE_RELEVANCE_AUDIT.md) R1) |
| `V1065-V1069` | - | Kept free (the EUA adapter configuration and a single-session rule if UQ04 / UQ09 require schema) |
| `db/seed/V1960__seed_user_access.sql` | U1-A | Users `requestor` (UAM_REQUESTOR), `uamapprover` (UAM_APPROVER), `secapprover` (UAM_SECOND_APPROVER). Requests in every status: draft, pending for `uamapprover`, returned, cancelled, scheduled (future date), approved and applied, a bulk batch of three lines, a CREATE_ROLE request FOR_IMPLEMENTATION for `admin`, a privileged change awaiting second approval. Change-log rows for the applied ones |
| `db/seed/V1961__seed_insurer_story_roles.sql` | Insurer suite hiding | Seed-only roles `SIT_INS_<role>` that keep the insurer access of the SIT/UAT users (`uw`, `claims`, `reinsurer`, `fmanager`, `accountant`, `checker`, `auditor`) after V1064, until the insurer modules are removed |

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

> **26-Sep-2026: dormant.** BDOI's drop plan places "Employee Benefits (no portal feature)" in Drop 2 (register DCR-211,
> IQ22), so no `portal` module is built and there are no external users. The built default of
> `ExternalUserProvisioner` keeps refusing EXTERNAL requests (`NbadminPortDefaults`). The rules below stay as the design
> if a portal is ever added.

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

### 6.2 Roles (V1060) and SIT/UAT users (V1960)

| Role | Persona | Permissions | SIT/UAT user |
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
| EIAM (Entra ID) sign-in | IQ04; DCR-230 | Section 10.1. Replaces the EUA password bind as the target; `DirectoryAuthenticator` stays for a break-glass local administrator |
| UIDM-ISC (IGA) provisioning | IQ05; DCR-229 | Section 10.1. None today; `ExternalUserProvisioner` serves portal users only |

### 10.1 EIAM (Entra ID) sign-in and UIDM-ISC (IGA) provisioning (Drop 0 integrations, 26-Sep-2026)

**EIAM sign-in (INT-01).** BDOI's Enterprise Identity Access Management runs on Microsoft Entra ID. BIBS signs BDO
users in with OpenID Connect (authorisation code with PKCE):
- the login page redirects to Entra ID; the backend validates the ID token (issuer, audience, signature from the tenant
  JWKS, nonce) with Spring Security OAuth2 and maps the identity to `sec_user` by `windows_id` or the UPN claim;
- a user unknown to BIBS, inactive or dormant is refused with the existing messages; roles and permissions stay in BIBS
  (group claims are not used for authorisation unless BDOI asks, IQ04);
- BIBS then issues its own JWT as today, so the permission checks, the session log, the idle warning and the 30-minute
  inactivity log-out (UAM-NFR) are unchanged; log-out also ends the Entra session (front-channel log-out);
- password, lockout and MFA rules move to Entra ID; `AUTH_MODE` gets the value `OIDC` and the password parameters apply
  only to the break-glass local administrator (LOCAL mode, audited);
- the port family becomes `SsoTokenExchange` (designed in section 10, built with EIAM); `DirectoryAuthenticator` stays for
  the local mode.

Open with BDOI IT (IQ04): tenant and app registration, claims (UPN or Windows ID), MFA and conditional access, token and
session lifetime against the 30-minute inactivity rule, sign-in of non-BDO users.

**UIDM-ISC provisioning (INT-02).** BDO's IGA tool (read as SailPoint Identity Security Cloud, to confirm) provisions
joiners, movers and leavers and certifies access. This conflicts with principle 2 ("a request in BIBS is the only way to
change access"; BRD-11 p.6). Options for BDOI (IQ05, DCR-229):

| Option | Requests and approvals | BIBS build |
|---|---|---|
| (a) IGA provisions user accounts, BIBS keeps role requests (proposal) | Create, deactivate and reactivate users come from UIDM-ISC; role (group-profile) and data-scope changes stay as BIBS requests under four eyes | SCIM 2.0 `/Users` endpoint (or the ISC connector) mapped to the `nbadmin` request lifecycle as system requests with the ISC request number; aggregation export of accounts and roles for certification |
| (b) IGA provisions users and roles | All access changes requested and approved in UIDM-ISC; BIBS applies them | SCIM `/Users` and `/Groups` (roles as entitlements); BIBS requests kept only for the break-glass path |
| (c) No IGA provisioning | BIBS requests as built; UIDM-ISC only aggregates for certification | Aggregation export only |

Every change applied from the IGA writes `sec_access_change_log` rows as today, with source `IGA` and the ISC reference,
so the BRD-11 audit reports keep working.

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

| Wave | Team | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **U0** (1 team, short; **the same foundation team as S0 of BRD-10**, or run after it) | User access foundation | Permissions, `AuditAction.LOGOUT`, security domain extensions, change log writes, role activation and effective permissions, direct role-edit guard, V1060, V1061, nav section registration (stub), crons, Developer Guide range rows | `security/domain/**`, `security/service/UserAdminService.java`, `security/service/UserDirectory.java`, `security/api/UserAdminController.java`, `security/api/dto/UserRequest.java`, `audit/domain/AuditAction.java`, `V1060`, `V1061`, `navigation/modules.ts`, `help/helpContent.ts`, `features/nbadmin/userAccessModule.ts`, `application.yml`, `docs/operations/CONFIGURATION.md` | `mvn verify` green; existing security and nbadmin tests unchanged, plus tests for inactive roles and the change log |
| **U1-A** | Request lifecycle and screens | `nbadmin` lifecycle, types, user type EXTERNAL and the `ExternalUserProvisioner` port with its refusing default (committed first, for EB E1-A), approvers, second approval, effective-date job, bulk handler, implementation flow, approval source, frontend User Access screens, admin Users / Roles screen changes, seed V1960 | `nbadmin/domain/**`, `nbadmin/service/Access*`, `nbadmin/service/bulk/**`, `nbadmin/api/Access*`, `V1062`, `db/seed/V1960`, `features/nbadmin/**` (except reports), `features/admin/UsersPage.tsx`, `RolesPage.tsx` | Draft -> submit -> return -> correct -> approve -> applied; scheduled -> applied by the job; group profile -> FOR_IMPLEMENTATION -> implemented; bulk batch of 3 |
| **U1-B** | Authentication, passwords, sessions, reports | Directory port and LOCAL adapter, logout, session log, `jti`, password history / age / forced change / self-service reset, own profile edit, batch-failure e-mail, the five reports | `security/service/Auth*`, `JwtTokenService.java`, `JwtAuthenticationFilter.java`, `security/service/directory/**`, `security/api/AuthController.java`, `system/service/JobFailureListener.java`, `nbadmin/report/**`, `features/profile/**`, `auth/**`, the sign-in page | Logout audited; the audit report shows from / to; password rules enforced; sessions listed |
| **U2** | Hardening | E2E, `ApiSmokeIT` entries, update of `BROKING_ARCHITECTURE.md` section 8 (by its owner), fit/gap refresh | tests + docs | Full `mvn verify` / `npm run verify` |

Rules for parallel work:
- One Flyway file set per team: U0 V1060 / V1061; U1-A V1062 / V1960.
- `security/domain/**` belongs to U0. After U0 merges, U1-B changes only the security service / api files listed for it.
- U1-A and U1-B both read `UserAdminService`; any change to it after U0 goes through U1-A.
- Shared files (the Permission enum, nav, help registry, `application.yml`) are edited **only in U0 / S0**.
- BRD-10 (Sanction Screening) S0 edits the same shared files, so S0 and U0 are **one team** or run one after the other.

## 14. What depends on information BDOI has not given

| Item | Question | Built now | Parked |
|---|---|---|---|
| EUA / AD / SSO | Q42, UQ04 | Port, LOCAL mode, `windows_id` field | Adapter; target is EIAM (Entra ID) OIDC, section 10.1 (IQ04) |
| IGA provisioning (UIDM-ISC) | IQ05, DCR-229 | BIBS requests as built | SCIM endpoint and aggregation export, section 10.1 |
| Approver population and rules | UQ01, UQ02 | Named approver, eligibility by permission, `UAM_ANY_APPROVER` | Per-unit approver lists |
| Implementation step for group profiles | UQ03 | FOR_IMPLEMENTATION step; `UAM_ROLE_APPLY_ON_APPROVAL` | - |
| Risky-change rules | UQ07 | Privilege levels, working-hours parameter, second approval | Final levels per role |
| Password values | UQ08 | Parameters seeded with the CLXN values | - |
| Single session per device | UQ09 | Session log | Enforcement |
| Business unit / user level values | UQ05 | LOVs (empty) | Values |

## 15. Risks

1. **Behaviour change for administrators.** SYSADMIN can no longer edit roles directly. Mitigation: the emergency parameter (audited, alert) and a clear "Implement request" flow; announce it in the release notes.
2. **Directory authentication unknown.** Mitigation: the port and the LOCAL mode; the Windows ID is captured now, so the switch is a configuration change.
3. **Shared security files.** The two BRD foundations and the Collections / Accounting build teams touch `Permission.java`. Mitigation: one foundation team, additive changes only, deliver early.
4. **Inactive roles.** A deactivated role could remove rights from active users by surprise. Mitigation: the request shows the members affected, and UQ16 decides whether deactivation is blocked while the role has members.

## 16. U0 foundation: as built

What the U0 wave built (together with S0 of BRD-10, one foundation team), and where it differs from or details the
sections above. U1-A and U1-B build on this.

- **Existing behaviour kept by default.** Every designed behaviour change is behind its parameter with today's
  behaviour as the seed data, until BDOI answers and U1-A delivers the implementation flow:
  - `UAM_DIRECT_ROLE_EDIT` = **true**: the Roles screen still creates and edits roles directly. Each such edit is
    audited ("Role created / changed directly (emergency path UAM_DIRECT_ROLE_EDIT)") and raises the alert
    `UAM_DIRECT_ROLE_EDIT` (one live alert per role). With false, `POST /admin/roles` and `PUT /admin/roles/{id}` are
    refused with `ROLE_EDIT_BY_REQUEST` unless they carry `?requestNo=` of an approved group-profile request.
    **U1-A** switches the seed to false in V1062 together with the "Implement request" flow and adapts
    `UserAdminApiIT` (it edits roles directly today).
  - `UAM_ROLE_APPLY_ON_APPROVAL` = **true**: approved role-permission requests (PMADD05) apply at once, as today.
    U1-A reads it in `AccessChangeApplier` (false = FOR_IMPLEMENTATION; UQ03).
- **Migration `V1060__uam_foundation.sql`.** Roles `UAM_REQUESTOR`, `UAM_APPROVER`, `UAM_SECOND_APPROVER` with the
  grants of 6.2; BUSINESS_ADMIN, SYSADMIN and AUDITOR grants of 6.2; every role holding ACCESS_REQUEST today also gets
  UAM_ENROLL / MODIFY / DEACTIVATE / REACTIVATE / CORRECT / CANCEL / VIEW / GROUP_REQUEST, and every role holding
  ACCESS_APPROVE gets UAM_VIEW. `sec_permission_action`: the `UAM_*` permissions, ACCESS_REQUEST / ACCESS_APPROVE moved
  from BROKING_ADMIN to `USER_ACCESS`, USER_MANAGE / ROLE_MANAGE (USER_ACCESS, CREATE + AMEND) and the platform
  administration permissions AUDIT_VIEW, SYSTEM_MONITOR, ALERT_VIEW, ALERT_MANAGE, SYSTEM_PARAMETER_MANAGE (area
  `ADMINISTRATION`). **Not classified:** the finance permissions (journals, periods, sub-ledgers, reinsurance, tax,
  budget, assets, FRBS pack): the existing Product Maintenance test pins JOURNAL_CREATE as unclassified, and the
  finance owner maps them (the group-profile report shows them under "Other" until then). Parameters of section 8
  (JOB_FAILURE_RECIPIENTS is a STRING of e-mail addresses); `LOGIN_MAX_FAILED_ATTEMPTS` description "all users"
  (CQ23, D5). LOVs `UAM_BUSINESS_UNIT`, `UAM_USER_LEVEL` (empty, UQ05), `UAM_DEACTIVATION_REASON`. Alert codes and
  notification events of section 8 (sort orders 600-660).
- **Migration `V1061__security_user_access_extensions.sql`.** `sec_user` + `windows_id` (unique, case-insensitive,
  partial index), `business_unit_code`, `user_level`, `password_changed_at`, `must_change_password`, `last_logout_at`;
  `sec_role` + `active`, `description`, `privilege_level` (LOW / STANDARD / HIGH / ADMIN; SYSADMIN = ADMIN),
  `deactivated_at/by`; `sec_access_change_log` (insert-only trigger; subject by value, no foreign key; subject types
  USER / ROLE, V1062 widens to EXTERNAL_USER); `sec_user_session` (`session_id` = jti, `expires_at` added so "online"
  needs no token); `sec_password_history` (by user name).
- **security domain.** `AppUser`: the new fields, `changePassword(hash, when, mustChange)`, `recordLogout(when)`;
  `effectivePermissions()` skips inactive roles. `Role`: `isActive()`, `deactivate(user, when)` /
  `reactivate()` (`ROLE_ALREADY_INACTIVE` / `ROLE_ALREADY_ACTIVE`), `description`, `PrivilegeLevel privilegeLevel`.
  `AccessChangeLog` + `AccessChange`, `AccessChangeSource`, `AccessChangeActivity`, `AccessSubjectType`,
  `AccessChangeLogRepository` (by subject; by request number); `UserSession` + `SessionEndReason`,
  `UserSessionRepository`; `PasswordHistory` + repository. Inactive roles are skipped by
  `RolePermissionLookup` (authorities), `UserDirectory.roleCodes` and `usersWithPermission`.
- **security services.**
  - `UserAdminService`: every change writes the change log through `AccessChangeRecorder` in the same transaction
    (one row per changed attribute: fullName, email, homeBranchId, authorizationLimit, windowsId, businessUnitCode,
    userLevel, roles (ROLES_CHANGED), enabled (DISABLE_USER / ENABLE_USER); roles: name, description, privilegeLevel,
    permissions (ROLE_PERMISSIONS); UNLOCK; PASSWORD_RESET; DEACTIVATE_ROLE / REACTIVATE_ROLE with attribute active).
    Overloads with a `ChangeAuthority(requestNo, approvedBy)` (`ChangeAuthority.DIRECT` for none):
    `createUser(request, password, authority)`, `updateUser(id, request, authority)`, `createRole(request, authority)`,
    `updateRole(id, request, authority)`, new `deactivateRole(id, authority)`, `reactivateRole(id, authority)`,
    `getRole(id)`. The existing signatures delegate with DIRECT. Password changes keep the history and
    `password_changed_at`; a password set by someone else (creation, admin reset) sets `must_change_password`, the
    user's own change clears it. **Not built (U1-B):** the reuse / age checks against `PASSWORD_HISTORY_COUNT`,
    `PASSWORD_MIN_AGE_DAYS`, `PASSWORD_MAX_AGE_DAYS` and the flag in `LoginResponse`.
  - `UserRequest` + optional `windowsId`, `businessUnitCode`, `userLevel` (null keeps the value on update, blank
    clears it; a Windows ID used by another user is refused with `WINDOWS_ID_IN_USE`); the 7-argument constructor stays.
    `RoleRequest` + optional `description`, `privilegeLevel` (3-argument constructor stays). `RoleResponse` + `active`,
    `description`, `privilegeLevel`; `UserProfileResponse` + `windowsId`, `businessUnitCode`, `userLevel`,
    `mustChangePassword`, `lastLogoutAt`.
  - `RoleEditGuard` (`authorize(roleCode, requestNo)`, `directEditAllowed()`, `directEditUsed(roleCode, change)`) and
    the port `security.service.ApprovedRoleRequests` (`Optional<String> approverOf(requestNo, roleCode)`; default
    `SecurityPortDefaults.noApprovedRoleRequests` knows none). **U1-A implements the port** in nbadmin for
    FOR_IMPLEMENTATION requests. Event `security.service.DirectRoleEditUsed(roleCode, change, actor)`, turned into the
    alert by `nbadmin.service.DirectRoleEditAlerts`.
  - `UserSessionLog`: `open(jti, username, expiresAt)` at login (`AuthService.login`), `end(jti, reason)` at logout
    (`AuthService.logout`, reason LOGOUT; the user's `last_logout_at` is set), `touch(jti)` (at most every 300 s),
    `isOnline(username)`, `sessionsOf(username)`. **U1-B** calls `touch` from `JwtAuthenticationFilter` and ends
    sessions on idle timeout, expiry, lock and admin end.
- **Navigation.** Group Setup & Administration, section **User Access** before Administration
  (`features/nbadmin/userAccessModule.ts`, module id `user-access`, help id `user-access`, `USER_ACCESS_HELP` in
  `features/nbadmin/help.ts`): `/user-access/requests` "Access Request Queues" (UAM_VIEW, also ACCESS_REQUEST,
  ACCESS_APPROVE, UAM_SECOND_APPROVE), a landing page with links to the Access Requests and User Access Matrix
  screens, which stay in Broking Setup until U1-A moves them (old routes redirect).
- **Jobs.** `brokerverse.jobs.uam-effective-changes-cron` `0 5 16 * * *` (00:05 PHT) and
  `password-expiry-notice-cron` `0 0 22 * * *` (06:00 PHT) in `application.yml` and `CONFIGURATION.md`.
- **Flyway left to the build waves.** U1-A V1062 and seed V1960; V1063-V1069 and V1961-V1969 free.

## 17. U1-A request lifecycle and screens: as built

What wave U1-A built on the U0 foundation, and where it differs from or details sections 4-11.

- **Port first (decision D7).** `nbadmin.service.ExternalUserProvisioner` (`available()`, `validate(action, account)`,
  `create`, `disable`, `enable` with `ExternalUserAccount(requestNo, username, fullName, email, partyKind, partyCode,
  portalRole, approvedBy)`) and its refusing default `NbadminPortDefaults.noExternalUsers()`
  (`@ConditionalOnMissingBean`; every call throws `EXTERNAL_USERS_NOT_AVAILABLE`). `portal` (EB E1-A) registers a bean
  of the interface; `nbadmin` never depends on `portal`. The port is also called on submission (`validate`), so an
  EXTERNAL request is refused before it reaches an approver. Enums `AccessUserType` and `ExternalPartyKind` are in
  `nbadmin.domain`.
- **Migration `V1062__nbadmin_request_lifecycle.sql`.** The columns, statuses and types of section 4.2 (with
  `reason_code`, `unlock_account`, `cancelled_by/at` and `apply_error` in addition); `nba_access_request_approver`
  (unique sequence per request, deferred so a resubmission replaces the list), `nba_access_request_event` (insert-only
  trigger; the history of the requests created before the release is back-filled), `nba_access_request_batch`;
  `sec_access_change_log.subject_type` + EXTERNAL_USER; BULK_PROCESS for UAM_REQUESTOR (the bulk file goes through
  the bulk upload framework); retention rule ACCESS_REQUEST (CANCELLED, REJECTED; REVIEW only). **Switches:**
  `UAM_DIRECT_ROLE_EDIT` = false and `UAM_ROLE_APPLY_ON_APPROVAL` = false (sections 8 and FRS 9.1).
- **Lifecycle (`nbadmin.service`).** `AccessRequestService` (drafts with format checks, edit, submit / resubmit with
  the chosen approvers, visibility, work-list tabs MINE / ASSIGNED / SECOND / IMPLEMENTATION / ALL),
  `AccessDecisionService` (approve in order, second approval, reject; applies now, SCHEDULED or FOR_IMPLEMENTATION),
  `AccessRequestReturnService` (return, compatible resubmit, cancel), `AccessImplementationService` (implement; the
  adapter of the security port `ApprovedRoleRequests`), `AccessScheduledChanges` + job `UAM_EFFECTIVE_CHANGES`,
  `AccessRiskRules` + `WorkingHours` (PRIVILEGE_INCREASE: a new HIGH / ADMIN profile for a user, a new HIGH / ADMIN
  profile, a level raised to HIGH / ADMIN, the reactivation of a HIGH / ADMIN profile; OUTSIDE_HOURS in Philippine
  time), `AccessApprovers`, `AccessRequestHistory` (event + audit trail), `AccessRequestNotifier` (the events of
  section 8; the affected user also by e-mail), `AccessBatchService` and the bulk handler
  `nbadmin.service.bulk.AccessRequestBulkHandler` (`UAM_ACCESS_REQUEST`).
- **Compatibility.** `POST /access-requests` without approvers still creates and submits in one call (PMADD05
  screens); such a request has no chosen approver and any holder of ACCESS_APPROVE decides it. With approvers, or with
  `?draft=true`, the BRD-11 lifecycle applies. The request endpoints check the type permission or ACCESS_REQUEST.
- **Implementation (PQ17).** Two paths close a FOR_IMPLEMENTATION request: `POST /access-requests/{id}/implement`
  (the Roles screen "Implement Request" and the request page) applies the approved change; and a role change made
  with `?requestNo=` on `/admin/roles` (`RoleEditGuard` asks `ApprovedRoleRequests.approverOf`, which knows only
  FOR_IMPLEMENTATION requests of that role, never for their requester). Both publish
  `security.service.RoleChangedOnRequest(requestNo, roleCode, actor)` from `UserAdminService` (new event, in the
  transaction of the change) and `AccessImplementationService` marks the request IMPLEMENTED.
- **Endpoints (`/api/v1/nbadmin`).** `GET access-requests` (scope, status, type, text, requester, approver, from, to,
  groupProfiles, page), `GET access-requests/{id}`, `GET .../{id}/history`, `POST access-requests[?draft=true]`,
  `PUT .../{id}`, `POST .../{id}/submit`, `.../approve`, `.../second-approve`, `.../reject`, `.../return`,
  `.../resubmit`, `.../cancel`, `.../implement`, `GET approvers?userType=&subject=`, `GET access-settings`,
  `GET access-batches`, `GET access-batches/{id}`, `GET .../{id}/lines`, `POST .../{id}/submit | approve | reject |
  return | cancel`. The bulk file itself is uploaded through `/api/v1/bulk/jobs` (handler `UAM_ACCESS_REQUEST`), not
  a separate multipart endpoint.
- **Screens.** User Access: Access Requests (`/user-access/requests`, tabs), New / Edit Request
  (`/user-access/requests/new`, `/:id/edit`), request page (`/user-access/requests/:id`: summary, actions, tabs
  Details / Approvers / History), Group Profile Requests (`/user-access/group-profiles`), Bulk Request
  (`/user-access/bulk`, batch page `/user-access/bulk/:id`), User Access Matrix (`/user-access/matrix`). The old
  Broking Setup routes redirect. Administration > Users: Windows ID, business unit, user level, status filter,
  "Raise Request" (direct create / edit only while `UAM_DIRECT_ROLE_EDIT` is open); Roles & Permissions: privilege
  level, active flag, "Approved Requests to Implement", read-only matrix unless the emergency path is open.
- **Seed `V1960__seed_user_access.sql`.** Users `requestor`, `uamapprover`, `secapprover` and subject users
  a013000101-104; requests AR-2026-900001 to 900008 in every status and the batch BLK-2026-900001 of three lines; change
  log rows of the applied enrolment. It opens `UAM_WORKING_HOURS` to the whole week in the seed and test database
  (the automated tests run at any hour); the delivered value stays 08:00-18:00,MON-FRI in V1060.
- **Parked / not built.** A scheduled enrolment creates the user with a password that is never shown: the System
  Administrator resets it (the self-service reset is U1-B). Business unit and user level codes are not checked against
  their lists (empty until UQ05). A deactivation with members is allowed (UQ16). Members of a deactivated or changed
  profile are not notified one by one (the requester is). The five reports are U1-B.

## 18. U1-B sign-in, passwords, sessions and reports: as built

What wave U1-B built on U0 and U1-A, and where it differs from or details sections 4-11.

- **Migration `V1063__security_sign_in_and_passwords.sql`.** `sec_user.mobile_no` (UQ17); `sec_password_reset_token`
  (SHA-256 of the token only, `expires_at`, `used_at`); notification event `PASSWORD_EXPIRY_NOTICE` (sort 670);
  `REPORT_VIEW` for every role holding `UAM_REPORT_VIEW` (UAM_APPROVER had none, so it could not reach the report
  runner). No seed migration.
- **Directory port (FR-UA-003; D6).** `security.service.directory`: `DirectoryAuthenticator` (`mode()`,
  `authenticate(userId, char[] password)` returning `DirectoryResult` SUCCESS / INVALID / LOCKED / ERROR with the
  directory's message), the LOCAL adapter `LocalPasswordAuthenticator` (the Spring authentication manager, BCrypt),
  `DirectoryAuthenticators` (the adapter of the mode; clears the password array) and `AuthMode` (`AUTH_MODE`). In
  DIRECTORY mode `AuthService.login` finds the user by `windows_id`; INVALID counts towards the lockout and shows the
  directory's message, LOCKED is shown as it is, ERROR refuses with `SIGN_IN_UNAVAILABLE` (HTTP 422) and counts
  nothing. **Parked:** the EUA / LDAP adapter (a bean of the interface with mode DIRECTORY, UQ04); until it exists
  DIRECTORY mode refuses every sign-in with "Directory sign-in is not available. Contact your administrator."
- **Passwords (UAM-NFR-31, 36, 37; FR-UA-005).** `AuthPasswordPolicy` (history `PASSWORD_HISTORY_COUNT` plus the
  current password: `PASSWORD_REUSED` "You used this password recently. Choose another one"; minimum age
  `PASSWORD_MIN_AGE_DAYS`: `PASSWORD_CHANGED_TOO_SOON` "You changed your password less than a day ago", not after a
  password set by someone else; maximum age `PASSWORD_MAX_AGE_DAYS`; none of it in DIRECTORY mode:
  `PASSWORD_MANAGED_BY_DIRECTORY`). `AuthPasswordService`: own change (`POST /auth/change-password` now goes here;
  `UserAdminService.changeOwnPassword` is left unchanged and unused by the API), the password status, the reset link
  and the expiring passwords. `LoginResponse` + `mustChangePassword`, `passwordChangeReason` (RESET after creation or
  an administrator reset, EXPIRED past the maximum age; a password without a change date never expires, so the seed
  users are not forced). The web client shows the forced change before the home page. Passwords set before V1061
  have no `password_changed_at` and never expire until changed once.
- **"Forgot password?"** `POST /auth/password-reset/request` `{userId}` (202 for every user ID; LOCAL mode, enabled
  users with an e-mail address; older links withdrawn), `POST /auth/password-reset/check` `{token}`,
  `POST /auth/password-reset/confirm` `{token, newPassword}` (history rule; `RESET_LINK_INVALID`,
  `RESET_LINK_EXPIRED`). Anonymous and CSRF-exempt like login (`SecurityConfig`). The link is
  `brokerverse.security.password-reset-url` (default: first allowed origin + `/reset-password`) `?token=`; the event
  `security.service.PasswordResetRequested` is e-mailed by `nbadmin.service.PasswordNoticeMailer` (purpose
  PASSWORD_RESET), because `security` does not depend on `messaging`. A reset link does not unlock a locked account.
- **Password expiry notice.** Job `PASSWORD_EXPIRY_NOTICE` is `nbadmin.service.PasswordExpiryNoticeJob` (same cron
  property): users whose password expires within 7 days get an in-app notice and an e-mail (event
  `PASSWORD_EXPIRY_NOTICE`, preferences respected).
- **Sessions (UAM-NFR-35; FR-UA-002, 004).** `JwtAuthenticationFilter` asks `UserSessionLog.check(jti)` on every
  request: an ended session is refused (401) even without the denylist, an open one is touched (every 5 minutes); a
  token of a user found locked or disabled ends its session (LOCKED / ADMIN_ENDED). The third failed login ends the
  user's open sessions (LOCKED). `POST /auth/logout?reason=IDLE_TIMEOUT|EXPIRED` records the web client's inactivity
  and end-of-token sign-outs (audit "Logged out after inactivity" / "at the end of the session"). Job
  `USER_SESSION_SWEEP` (`brokerverse.jobs.user-session-sweep-cron`, every 15 minutes) ends sessions idle longer than
  `SESSION_TIMEOUT_MINUTES` + 5 minutes, expired, or of locked / disabled users. The web client keeps the server
  session alive on activity (at most every 4 minutes). Endpoints: `GET /auth/sessions` (own), `GET /admin/sessions`
  (`username`, `open`, page), `GET /admin/sessions/online`, `POST /admin/sessions/{sessionId}/end` (USER_MANAGE;
  revokes the token, ADMIN_ENDED, audited as LOGOUT by the administrator; `SESSION_ALREADY_ENDED`).
- **Own profile (UQ17).** `PUT /auth/me` `{email, mobileNo}` with field errors (`PROFILE_INVALID`); each changed
  attribute is a MODIFY_USER row of the change log done by the user, without a request. `GET /auth/password-status`.
  `UserProfileResponse` + `mobileNo`.
- **Batch failure e-mail (UAM-NFR-25; FR-UA-071).** `messaging.service.JobFailureMailer` implements the port
  `system.service.JobFailureListener` (a listener in `system` would create a `system` - `messaging` cycle): every
  failed run is e-mailed to the valid addresses of `JOB_FAILURE_RECIPIENTS` (invalid ones are skipped and logged),
  purpose JOB_FAILURE, besides the JOB_FAILURE alert.
- **Reports (`nbadmin.report`, category Control & Audit, view and export `UAM_REPORT_VIEW`, archived).**
  `UAM-USER-ACCESS`, `UAM-GROUP-PROFILE`, `UAM-GROUP-MEMBERS`, `UAM-AUDIT-LOG`, `UAM-REQUESTS` (section 11.1). The
  as-of views undo the later rows of the change log (`UserAccessHistory`); "created / modified by" is
  "approver (request no.)" or the user of a direct change. The audit log merges the change log (activity words of
  sample D, role codes shown as profile names, "Null" for no value), the request history and, with
  `includeSignIns`, LOGIN / LOGIN_FAILED / LOGOUT of the audit trail. `AS_OF_IN_FUTURE` "The as-of date cannot be in
  the future"; `DATE_RANGE_REVERSED` "The end date must be on or after the start date". The group profile report
  lists the modules in which a profile has at least one task unless a module (area code) is chosen.
- **Screens.** Sign-in: "Forgot password?", `/reset-password` (public route), the forced change page (in
  `RequireAuth`). My Profile: contact details, password with its rules and dates (DIRECTORY mode: a notice), recent
  sessions. Administration > Users: status Online, Sessions dialog with End Session. User Access > User Access
  Reports (`/user-access/reports`, UAM_REPORT_VIEW) with help entry.
- **Not built / parked.** The EUA adapter and the SSO exchange (UQ04); the single session per device (UQ09, the
  log is ready); an IdP-specific "Forgot password?" (hidden only by the server's no-op in DIRECTORY mode: the login
  page cannot know the mode before sign-in); rate limiting of the reset request beyond the withdrawal of older
  links.

## 19. U2 integration and hardening: as built

- **End to end over HTTP.** `api/UserAccessEndToEndApiIT`: the Requestor enrols a user as SCR_INVESTIGATOR and the
  Approver approves (temporary password); the user signs in, must change the password, and `/auth/me` returns exactly
  the investigator's permissions; a group-profile change of the user submitted outside `UAM_WORKING_HOURS` gets the
  OUTSIDE_HOURS flag and needs the Second Approver; the Business Administrator's CREATE_ROLE request is implemented by
  the System Administrator; the deactivation ends the user's sessions at once; UAM-AUDIT-LOG shows the profile change
  with its from / to values, approver and request number.
- **Smoke.** `ApiSmokeIT.screeningAndUserAccessListsRespondOk` covers the list reads of `/nbadmin` per persona.
- **Persona menus (client requirement 14).** Shared with BRD-10: `frontend/src/navigation/personaMenus.json`,
  `navigation/personaMenus.test.ts`, `security/PersonaMenusIT`.
- **Fixes.**
  - `UserAdminService.updateUser` ends every open session of a user who becomes disabled (end reason ADMIN_ENDED).
    Before, the sessions stayed open (and the user "Online") until the token was used again or the 15-minute sweep.
  - The User Access Matrix was in the menu of every UAM_VIEW holder, but its endpoint refuses the Requestor and the
    Second Approver (403). The menu entry now follows the endpoint's permissions (ACCESS_REQUEST, ACCESS_APPROVE,
    ROLE_MANAGE, AUDIT_VIEW). Whether requestors should read the matrix is for BDOI to decide; opening it means adding
    UAM_VIEW to `AccessMatrixController`.
- **Guide.** [`docs/modules/USER_ACCESS.md`](../modules/USER_ACCESS.md) (with production-support troubleshooting); the
  as-built status per BRD ID is in `BDOI_UAM_BRD_SPEC.md`.
