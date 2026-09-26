# User Access Maintenance (BRD-11)

Guide of User Access Maintenance in iNXT BrokerVerse: access requests for users and group profiles
(`nbadmin`), and the sign-in, password, session and access-change log services they rely on
(`security`). The build design is
[`docs/architecture/USER_ACCESS_DESIGN.md`](../architecture/USER_ACCESS_DESIGN.md) (sections 16 to
18 record what each wave built); the requirements and their as-built status per BRD ID are in
[`docs/requirements/BDOI_UAM_BRD_SPEC.md`](../requirements/BDOI_UAM_BRD_SPEC.md); the functional
specification is
[`FRS_BRD11_USER_ACCESS_MAINTENANCE.md`](../deliverables/src/frs/FRS_BRD11_USER_ACCESS_MAINTENANCE.md).

## 1. Purpose

Every change of who can do what in BrokerVerse goes through a request that someone other than the
requester approves, and leaves a record of the value before and after:

- **user requests**: enrol a new user, modify a user (data or group profiles), deactivate, reactivate;
  internal users and, through a port, portal (external) users;
- **group-profile requests**: create a profile (role), change its access, deactivate, reactivate;
  the System Administrator implements the approved request;
- **bulk requests** from a file;
- a **second approval** for risky changes (a high-privilege profile, or outside working hours);
- **effective dates** (the change applies on the date, by a nightly job);
- sign-in with a **password policy** (history, minimum and maximum age, forced change, self-service
  reset), a **session log** (online status, sign-out, administrator "End Session"), and the five
  **user access reports** including the audit log with from / to values.

Roles are never edited freely: `UAM_DIRECT_ROLE_EDIT` is false, so `POST /admin/roles` and
`PUT /admin/roles/{id}` are refused (`ROLE_EDIT_BY_REQUEST`) unless they carry the number of an
approved request waiting for implementation.

## 2. Personas and roles

| Persona (BRD) | Role | SIT/UAT user | What they do |
|---|---|---|---|
| Requestor | `UAM_REQUESTOR` | `requestor` | Raises, edits, corrects, cancels user requests and bulk files; views own requests |
| Approver | `UAM_APPROVER` | `uamapprover` | Approves, rejects or returns the requests assigned to them; runs the reports |
| Second approver | `UAM_SECOND_APPROVER` | `secapprover` | Gives the second approval of risky changes (never the first approver) |
| Business Administrator | `BUSINESS_ADMIN` (+ UAM_GROUP_REQUEST, UAM_VIEW, UAM_REPORT_VIEW, UAM_CORRECT, UAM_CANCEL) | `badmin` | Raises group-profile requests; runs the reports |
| System Administrator | `SYSADMIN` (+ UAM_VIEW, UAM_REPORT_VIEW; ROLE_MANAGE, USER_MANAGE) | `admin` | Implements approved group-profile requests, unlocks, resets passwords, ends sessions |
| Auditor | `AUDITOR` (+ UAM_VIEW, UAM_REPORT_VIEW) | `auditor` | Reads requests and reports |
| BRD-1 approver | `NB_APPROVER` (ACCESS_APPROVE) | `approver` | Keeps deciding the product-maintenance access requests (PMADD05) |

Subject users of the seed data: `a013000101` to `a013000104`; requests AR-2026-900001 to 000008 in every
status and the bulk batch BLK-2026-900001 (V1960). The password is held in the seed configuration.

Segregation, enforced by the services: the requester and the subject user never decide a request
(`ACCESS_FOUR_EYES`, `ACCESS_SUBJECT_DECIDES`); the second approver differs from the first
(`ACCESS_SECOND_SAME_APPROVER`); the implementer of a group-profile request is not its requester
(`ACCESS_IMPLEMENTER_IS_REQUESTER`); nobody changes their own roles (`SELF_ROLE_CHANGE`).

The permissions of each role and the menu it sees are pinned by the persona check (client
requirement 14): [`frontend/src/navigation/personaMenus.json`](../../frontend/src/navigation/personaMenus.json),
tested by `navigation/personaMenus.test.ts` and `security/PersonaMenusIT`.

## 3. Screens

Group **Setup & Administration**, section **User Access** (`features/nbadmin/userAccessModule.ts`):

| Screen | Route | Permission | What it shows |
|---|---|---|---|
| Access Requests | `/user-access/requests` | UAM_VIEW (or ACCESS_REQUEST, ACCESS_APPROVE, UAM_SECOND_APPROVE, ROLE_MANAGE, AUDIT_VIEW) | Tabs My Requests / Assigned to Me / Second Approval / For Implementation / All; search and filters (status, type, requester, approver, dates) |
| New / Edit Request | `/user-access/requests/new`, `/:id/edit` | UAM_ENROLL or another request function | One form for the request types: user, data, group profiles, effective date, approver(s), remarks; Save Draft / Submit / Cancel |
| Access Request | `/user-access/requests/:id` | as Access Requests | Summary, actions by status and role (approve, second approval, reject, return, resubmit, cancel, implement), tabs Details / Approvers / History |
| Group Profile Requests | `/user-access/group-profiles` | UAM_GROUP_REQUEST (or the viewers above) | Create / change / deactivate / reactivate profiles with the permission picker by area and action; Implement (System Administrator) |
| Bulk Request | `/user-access/bulk` | UAM_ENROLL, UAM_MODIFY or ACCESS_APPROVE | Template, upload (bulk framework, handler `UAM_ACCESS_REQUEST`), batches; batch page `/user-access/bulk/:id` |
| User Access Matrix | `/user-access/matrix` | ACCESS_REQUEST, ACCESS_APPROVE, ROLE_MANAGE or AUDIT_VIEW (not the Requestor or the Second Approver) | Profiles x permissions and x action classes; export |
| User Access Reports | `/user-access/reports` | UAM_REPORT_VIEW | Links to the five reports in the report runner |

Also used: Administration > **Users** (Windows ID, business unit, user level, status with Online,
Sessions dialog with End Session, "Raise Request"), Administration > **Roles & Permissions**
(privilege level, active flag, "Approved Requests to Implement"), **My Profile** (contact details,
password with its rules and dates, recent sessions), the sign-in page ("Forgot password?"),
`/reset-password` and the forced password change. The old Broking Setup routes of access requests
and the matrix redirect. Help: `features/nbadmin/help.ts` (`USER_ACCESS_HELP`).

## 4. Flows

### 4.1 Request life cycle

| Status | Reached by | Next |
|---|---|---|
| DRAFT | Save Draft (`POST /access-requests?draft=true`) | Edit, Submit, Cancel |
| PENDING | Submit with the chosen approver(s) | Approve (in approver order), Reject, Return, Cancel (requester) |
| RETURNED | Return with remarks | Correct and Resubmit, Cancel |
| PENDING_SECOND | Approve of a request with a risk flag | Second approval (UAM_SECOND_APPROVE), Reject |
| SCHEDULED | Approve with a future effective date | Applied by `UAM_EFFECTIVE_CHANGES` on the date |
| FOR_IMPLEMENTATION | Approve of a group-profile request (`UAM_ROLE_APPLY_ON_APPROVAL` = false) | Implement (System Administrator) |
| APPROVED / IMPLEMENTED | Applied | - |
| REJECTED / CANCELLED | Reject / Cancel with a reason | - |

On submission the request is validated: the user exists or not, the user ID follows
`USER_ID_PATTERN`, profiles exist and are active, one open request per user or profile, the
effective date is not in the past, the Windows ID is free, something actually changes. Every step
writes the request history (`nba_access_request_event`, insert-only) and the audit trail, and
notifies the next actor (events `UAM_REQUEST_TO_APPROVE`, `UAM_REQUEST_RETURNED`,
`UAM_SECOND_APPROVAL`, `UAM_FOR_IMPLEMENTATION`, `UAM_REQUEST_DECIDED`, `UAM_REQUEST_CANCELLED`;
the affected user gets `UAM_ACCESS_CHANGED` in-app and by e-mail).

A request created without approvers (the product-maintenance screens of BRD-3) is created and
submitted in one call and can be decided by any holder of ACCESS_APPROVE (compatibility).

### 4.2 Risk flags and second approval (UAM-NFR-40)

- **PRIVILEGE_INCREASE**: a HIGH / ADMIN profile given to a user, a new HIGH / ADMIN profile, a
  profile raised to HIGH / ADMIN, the reactivation of a HIGH / ADMIN profile.
- **OUTSIDE_HOURS**: submitted or approved outside `UAM_WORKING_HOURS` (Philippine time).

A flagged request raises the alert `UAM_PRIVILEGED_CHANGE` and, after the approver, waits for a
holder of UAM_SECOND_APPROVE (PENDING_SECOND, shown in My Approvals).

### 4.3 Applying a change

User requests apply at approval (or on the effective date): `UserAdminService` changes the user
and writes one row per changed attribute to `sec_access_change_log` (insert-only) with the request
number and the approver. An enrolment returns a temporary password shown once to the approver; the
user must change it at first sign-in. **A deactivation ends every open session of the user at
once** (session end reason ADMIN_ENDED), so the user is signed out everywhere and no longer shown
Online. Group-profile requests wait for implementation: "Implement Request" on the request or the
Roles screen applies the approved change and marks the request IMPLEMENTED.

### 4.4 Sign-in, passwords and sessions (UAM-NFR-35 to 37)

- `AUTH_MODE` LOCAL: BrokerVerse passwords (BCrypt). DIRECTORY: the directory port signs in by
  Windows ID; its adapter is parked, so DIRECTORY refuses every sign-in until it exists.
- Lockout after `LOGIN_MAX_FAILED_ATTEMPTS` (3) failed sign-ins; the third ends the user's sessions.
- Password history (`PASSWORD_HISTORY_COUNT` 8), minimum age 1 day, maximum age 90 days; a
  password set by someone else must be changed at the next sign-in (`mustChangePassword`,
  reason RESET or EXPIRED).
- "Forgot password?" sends a single-use link valid 30 minutes (LOCAL mode, users with an e-mail).
- Every sign-in opens a session row; each request touches it; sign-out, idle timeout, expiry, lock,
  deactivation and "End Session" end it. An ended session's token is refused (401).

The whole path is exercised through the HTTP API by `api/UserAccessEndToEndApiIT`: enrolment ->
approval -> first sign-in with the forced change -> exactly the role's permissions -> a change
outside working hours with the second approval -> a group profile implemented -> deactivation
ending the sessions -> the audit log with from / to values.

## 5. Jobs

| Job | Cron property (UTC; PHT time) | Work |
|---|---|---|
| `UAM_EFFECTIVE_CHANGES` | `brokerverse.jobs.uam-effective-changes-cron` `0 5 16 * * *` (00:05) | Applies the SCHEDULED requests due today or earlier; a failure raises `UAM_SCHEDULED_APPLY_FAILED` |
| `PASSWORD_EXPIRY_NOTICE` | `brokerverse.jobs.password-expiry-notice-cron` `0 0 22 * * *` (06:00) | In-app notice and e-mail to users whose password expires within 7 days (LOCAL mode) |
| `USER_SESSION_SWEEP` | `brokerverse.jobs.user-session-sweep-cron` `0 */15 * * * *` (default in code) | Ends idle, expired, locked and disabled users' sessions |

Every failed job run is also e-mailed to `JOB_FAILURE_RECIPIENTS`.

## 6. Parameters and lists of values

| Parameter (category SECURITY) | Delivered value | Use |
|---|---|---|
| `AUTH_MODE` | LOCAL | LOCAL or DIRECTORY (UQ04) |
| `USER_ID_PATTERN` | `^[a-zA-Z][0-9]{9}$` | Format of a new user ID (UQ05) |
| `PASSWORD_HISTORY_COUNT` / `PASSWORD_MIN_AGE_DAYS` / `PASSWORD_MAX_AGE_DAYS` | 8 / 1 / 90 | Password policy (UQ08) |
| `UAM_WORKING_HOURS` | `08:00-18:00,MON-FRI` (seed and test database: `00:00-24:00,MON-SUN`) | Out-of-hours flag; format `HH:mm-HH:mm,DAY-DAY`; blank or unreadable = always within hours |
| `UAM_ANY_APPROVER` | false | Any ACCESS_APPROVE holder may decide, not only the chosen approver (UQ02) |
| `UAM_DIRECT_ROLE_EDIT` | false | Emergency path: direct role edits, audited with alert `UAM_DIRECT_ROLE_EDIT` |
| `UAM_ROLE_APPLY_ON_APPROVAL` | false | Group-profile requests apply at approval instead of waiting for implementation (UQ03) |
| `JOB_FAILURE_RECIPIENTS` | empty | E-mail addresses of the job-failure e-mail |
| `LOGIN_MAX_FAILED_ATTEMPTS` | 3 | Lockout, all users |
| `SESSION_TIMEOUT_MINUTES` | platform value | Idle limit of the web client and the sweep |

Lists of values: `UAM_BUSINESS_UNIT`, `UAM_USER_LEVEL` (both empty until UQ05; not checked yet),
`UAM_DEACTIVATION_REASON` (RESIGNED, TRANSFERRED, LONG_LEAVE, SECURITY, OTHERS; to confirm).

## 7. Reports (category Control & Audit, view and export UAM_REPORT_VIEW)

| Code | BRD | Content | Parameters |
|---|---|---|---|
| `UAM-USER-ACCESS` | 3.003.1 | Users with IDs, profiles, business unit, level, status, created / modified by and date (as of a date) | as of, business unit, status, profile |
| `UAM-GROUP-PROFILE` | 3.003.2 | Profiles x modules x tasks, With / No Access | profile, area, active |
| `UAM-GROUP-MEMBERS` | 3.003.3 | Members of each profile, added / modified by and date | profile, as of |
| `UAM-AUDIT-LOG` | 4.003.1 | Date, activity, from, to, done by, approved by, request number; sign-ins optional | from, to, user, activity, include sign-ins |
| `UAM-REQUESTS` | 1.008 | Requests by status, type, requester, approver, age | period, status, type |

"Created / modified by" shows "approver (request no.)" or the user of a direct change. As-of
views undo the later rows of the change log.

## 8. Integrations and ports

| Port | Default | Replace when |
|---|---|---|
| `security.service.directory.DirectoryAuthenticator` | `LocalPasswordAuthenticator` (LOCAL) | BDO gives the EUA / LDAP / SSO details (UQ04): add a bean with mode DIRECTORY |
| `nbadmin.service.ExternalUserProvisioner` | `NbadminPortDefaults.noExternalUsers()`: refuses (`EXTERNAL_USERS_NOT_AVAILABLE`) | The portal module (Employee Benefits) registers its adapter |
| `security.service.ApprovedRoleRequests` | Implemented by nbadmin (`AccessImplementationService`) | - |
| `system.service.JobFailureListener` | `messaging.service.JobFailureMailer` | - |

Events: `security.service.RoleChangedOnRequest` (marks the request IMPLEMENTED),
`DirectRoleEditUsed` (alert), `PasswordResetRequested` (e-mailed by `nbadmin.PasswordNoticeMailer`).

## 9. Parked items

| Item | Question | Built now |
|---|---|---|
| EUA / LDAP / SSO adapter | Q42, UQ04 | Port, LOCAL mode, Windows ID captured; DIRECTORY mode refuses sign-in |
| External ACL | UQ14 | None |
| Single session per device | UQ09 | Session log only |
| Business unit and user level values | UQ05 | Empty lists, codes not checked |
| Deactivating a profile that still has members | UQ16 | Allowed; the members are not notified one by one |
| Final privilege levels per role and working hours | UQ07 | Levels on the roles, parameter |
| Rate limiting of the reset-link request | - | Older links are withdrawn |
| IdP-specific "Forgot password?" | UQ04 | Server no-op in DIRECTORY mode |
| Scheduled enrolment password | - | Never shown; the System Administrator resets it |

## 10. Troubleshooting (production support)

| Symptom / error code | Cause | Fix |
|---|---|---|
| `ACCESS_USER_ID_FORMAT` on submit | The user ID does not match `USER_ID_PATTERN` | Use the agreed format (letter + 9 digits by default) or correct the parameter |
| `ACCESS_USER_EXISTS` / `ACCESS_UNKNOWN_USER` | Enrolment of an existing user / change of a missing one | Use Modify (or Reactivate) instead of Enrol, or check the user ID |
| `ACCESS_REQUEST_PENDING` | Another request for the same user or profile is open | Decide or cancel the open request first |
| `ACCESS_NOTHING_CHANGED` | The request equals the current access | Change at least one attribute or profile |
| `ACCESS_WINDOWS_ID_IN_USE` / `WINDOWS_ID_IN_USE` | The Windows ID belongs to another user | Correct the Windows ID |
| `ACCESS_APPROVER_NOT_ELIGIBLE` | The chosen approver lacks ACCESS_APPROVE or is the requester / subject | Pick an approver from the list offered by the form |
| `ACCESS_NOT_ASSIGNED` | The request is assigned to another approver | That approver decides it, or set `UAM_ANY_APPROVER` = true if BDOI agrees (UQ02) |
| `ACCESS_FOUR_EYES` / `ACCESS_SUBJECT_DECIDES` | The requester or the subject user tried to decide | Another approver decides |
| `ACCESS_SECOND_APPROVAL_PENDING` | The request waits for the second approval | A holder of UAM_SECOND_APPROVE decides it (My Approvals) |
| `ACCESS_SECOND_SAME_APPROVER` | The first approver tried the second approval | Another second approver decides |
| Every request needs a second approval | `UAM_WORKING_HOURS` does not cover the current time (Philippine time), or the profile is HIGH / ADMIN | Check the parameter value and the profile's privilege level |
| `ROLE_EDIT_BY_REQUEST` on the Roles screen | Direct role edits are closed (`UAM_DIRECT_ROLE_EDIT` = false) | Raise a group-profile request; after approval use "Implement Request". In an emergency only, open the parameter (audited, alert) |
| `ROLE_REQUEST_NOT_APPROVED` / `ACCESS_REQUEST_NOT_FOR_IMPLEMENTATION` | The request number is not an approved request of this profile waiting for implementation | Check the request status and profile code |
| `ACCESS_IMPLEMENTER_IS_REQUESTER` | The requester tried to implement | Another System Administrator implements |
| SCHEDULED request not applied; alert `UAM_SCHEDULED_APPLY_FAILED` | The change was no longer valid on the date (user or profile changed) | Read `applyError` on the request; raise a new request; check the job monitor for `UAM_EFFECTIVE_CHANGES` |
| New user cannot sign in: "must change password" loop | The temporary password must be changed first | Change it on the forced-change page; the rules are shown there |
| `PASSWORD_REUSED` / `PASSWORD_CHANGED_TOO_SOON` | History or minimum age rule | Choose a password not among the last 8; wait one day after a change of your own |
| `RESET_LINK_INVALID` / `RESET_LINK_EXPIRED` | The link was used, withdrawn by a newer one, or is older than 30 minutes | Request a new link |
| No reset e-mail | The user has no e-mail address, is disabled, or `AUTH_MODE` is DIRECTORY | Add the e-mail through a Modify request, or the System Administrator resets the password |
| `SIGN_IN_UNAVAILABLE` / "Directory sign-in is not available" | `AUTH_MODE` = DIRECTORY without an adapter | Set `AUTH_MODE` back to LOCAL |
| User locked | 3 failed sign-ins | Administration > Users > Unlock (System Administrator) |
| 401 right after a sign-in elsewhere or after deactivation | The session was ended (End Session, lock, deactivation) | Expected; the user signs in again if still active |
| A user still shown Online after leaving | The browser was closed without signing out | The sweep ends the session after `SESSION_TIMEOUT_MINUTES` + 5 minutes; or End Session |
| User Access Matrix missing from the menu or answering 403 | The user holds none of ACCESS_REQUEST, ACCESS_APPROVE, ROLE_MANAGE, AUDIT_VIEW (UAM_VIEW alone does not open it) | Expected for the Requestor and the Second Approver; otherwise grant the role through a request |
| Report `AS_OF_IN_FUTURE` / `DATE_RANGE_REVERSED` | Parameter dates | Enter today or earlier / an end date on or after the start |

## 10.1 Where to look

- Tables: `nba_access_request`, `nba_access_request_approver`, `nba_access_request_event`,
  `nba_access_request_batch`; `sec_user`, `sec_role`, `sec_role_permission`, `sec_access_change_log`,
  `sec_user_session`, `sec_password_history`, `sec_password_reset_token`.
- Alerts (module USER_ACCESS): `UAM_PRIVILEGED_CHANGE`, `UAM_SCHEDULED_APPLY_FAILED`,
  `UAM_DIRECT_ROLE_EDIT`; platform `JOB_FAILURE`.
- Audit trail: LOGIN, LOGIN_FAILED, LOGOUT and every change of users, roles and requests.
