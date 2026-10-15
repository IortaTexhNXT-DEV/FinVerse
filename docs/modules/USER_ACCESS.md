# User Access Maintenance (BRD-11)

Guide of User Access in iNXT BrokerVerse after the move to UIDM-ISC and Microsoft Entra ID. The
build design is [`docs/architecture/USER_ACCESS_DESIGN.md`](../architecture/USER_ACCESS_DESIGN.md);
the requirements per BRD ID are in
[`docs/requirements/BDOI_UAM_BRD_SPEC.md`](../requirements/BDOI_UAM_BRD_SPEC.md).

## 1. Model

| What | Kept by | How it reaches BrokerVerse |
|---|---|---|
| Users (joiner, mover, leaver, rehire, status) | UIDM-ISC | SCIM 2.0 `/integration/v1/scim/v2/Users` (POST, PUT, PATCH, DELETE, GET) |
| Membership of the group profiles | UIDM-ISC | SCIM `PATCH /integration/v1/scim/v2/Groups/{id}` (add / remove members) |
| Sign-in and second factor | Microsoft Entra ID | OIDC; the identity is linked to an existing active user only |
| Group-profile definitions (permissions, name, level, active) | BrokerVerse | Maker proposes, another user approves; the System Administrator implements |
| Data access (companies, branches), unlock, end of sessions | BrokerVerse | User Access Administrator (USER_MANAGE) |

- No user is created or changed in BrokerVerse screens or API: `POST /api/v1/admin/users`,
  `PUT /api/v1/admin/users/{id}` and the creation from the directory are gone. A request about a
  user is refused (`ACCESS_USERS_BY_UIDM`).
- No break-glass account. Local passwords exist only for the test accounts of DEV, SIT and UAT;
  production refuses every local sign-in, password change and reset (`LocalPasswordSignIn`,
  `ProductionSafeguards`). `POST /api/v1/admin/users/{id}/reset-password` resets a test account
  only where local passwords are allowed (`SSO_PASSWORD_RESET` otherwise) and never one's own
  password (`OWN_PASSWORD_RESET`).
- Group-profile changes have no approver list and no effective date: any holder of
  ACCESS_APPROVE other than the maker decides; a risky change needs UAM_SECOND_APPROVE.

## 2. Personas

| Persona | Role | SIT/UAT user | Does |
|---|---|---|---|
| User Access Administrator | `UAM_ADMINISTRATOR` | `uamadmin` | Users (read, unlock, end sessions, data access), Identity Sync, SoD, matrix, reports, audit log, UAM Settings |
| Group Profile Maker | `UAM_REQUESTOR` | `requestor` | Proposes group-profile changes |
| Group Profile Checker | `UAM_APPROVER` | `uamapprover` | Approves, returns or rejects them |
| Second approver | `UAM_SECOND_APPROVER` | `secapprover` | Second approval of risky changes |
| Information Security | `INFOSEC_OFFICER` | `infosec` | SoD rules, security settings approval, certification oversight |
| Auditor (read-only) | `UAM_AUDITOR` (V2831) | `uamauditor` | Users, Group Profiles, Separation of Duties, Access Matrix, Reports & Certification, Audit Log; no action |
| System Administrator | `SYSADMIN` | `admin` | Implements approved group-profile changes |

The auditor reads the users with USER_VIEW (list, detail, group profiles, sessions, history,
data access); unlock, end sessions and data-access changes stay with USER_MANAGE. The general
`AUDITOR` role of the finance modules is unchanged; a user who audits User Access gets the
`UAM_AUDITOR` group profile from UIDM-ISC as well.

Permissions and menus per persona are pinned in
[`frontend/src/navigation/personaMenus.json`](../../frontend/src/navigation/personaMenus.json)
(`personaMenus.test.ts`, `PersonaMenusIT`).

## 3. Screens (Setup & Administration > User Access, `/access/*`)

| Screen | Permission | Content |
|---|---|---|
| Users | USER_MANAGE or USER_VIEW | List, filters, CSV; detail with profile from UIDM-ISC, group profiles, permissions, data access, sessions, history |
| Group Profiles | ROLE_MANAGE or the UAM viewers | Definition, permissions, members, pending change, history |
| Identity Sync | USER_MANAGE | Status, events, refused events, reconciliation, simulator (non-production) |
| Separation of Duties | UAM_SOD_MAINTAIN, UAM_SOD_AUTHORIZE, AUDIT_VIEW or USER_MANAGE | Rules and violations |
| Access Matrix | UAM_REPORT_VIEW | Profiles x permissions |
| Reports & Certification | UAM_REPORT_VIEW | Reports; certification extract (CSV/XLSX, logged with SHA-256) |
| Audit Log | AUDIT_VIEW | Audit trail preset to the user access records |
| UAM Settings | USER_MANAGE | Session, dormancy, test-account and synchronisation parameters (maker-checker) |

## 4. Group-profile change

DRAFT -> PENDING -> (PENDING_SECOND) -> FOR_IMPLEMENTATION -> IMPLEMENTED; RETURNED, REJECTED and
CANCELLED on the way. `UAM_ROLE_APPLY_ON_APPROVAL` = true applies at approval (APPROVED). One open
change per profile (`ACCESS_REQUEST_PENDING`). Every step is in `nba_access_request_event`, the
audit trail and `sec_access_change_log` (with request number and approver).

## 5. Jobs

| Job | Cron property (UTC) | Work |
|---|---|---|
| `UAM_DORMANT_USERS` | `brokerverse.jobs.uam-dormant-users-cron` `0 15 16 * * *` | Users without a sign-in for `UAM_DORMANT_DAYS` are deactivated directly (sessions ended, change log and audit "Deactivated: no sign-in for N days"); the USER_MANAGE holders are told to disable them in UIDM-ISC too; the user is warned `UAM_DORMANT_NOTICE_DAYS` before; SYSADMIN exempt |
| `UAM_IDENTITY_RECONCILIATION` | `brokerverse.jobs.identity-reconciliation-cron` | Compares the users with UIDM-ISC; mismatches on Identity Sync |
| `UAM_SOD_VIOLATIONS` | `brokerverse.jobs.uam-sod-violations-cron` | Records new SoD breaches, tells Information Security |
| `PASSWORD_EXPIRY_NOTICE` | `brokerverse.jobs.password-expiry-notice-cron` | Local test accounts only |
| `USER_SESSION_SWEEP` | `brokerverse.jobs.user-session-sweep-cron` | Ends idle, expired, locked and disabled sessions |

The effective-date job `UAM_EFFECTIVE_CHANGES` is gone (no effective dates).

## 6. Limits for 530 users

Lists are paged: SCIM `count` at most 200 per page; sessions, events and requests have a maximum
page size. The load scripts and the query plans are in [`perf/uam`](../../perf/uam/README.md).

## 7. Troubleshooting

| Symptom / code | Cause | Fix |
|---|---|---|
| `ACCESS_USERS_BY_UIDM` | A request about a user | Raise it in UIDM-ISC |
| `ACCESS_REQUEST_PENDING` | Another change of the profile is open | Decide or cancel it first |
| `ACCESS_FOUR_EYES` | The maker tried to decide | Another approver decides |
| `ACCESS_IMPLEMENTER_IS_REQUESTER` | The maker tried to implement | Another System Administrator implements |
| `SSO_PASSWORD_RESET` | Password reset where local passwords are off | Reset in Entra ID |
| Identity event REFUSED (alert `UAM_IDENTITY_REFUSED`) | Unknown group profile, invalid data | Identity Sync > Refused: retry or dismiss with remark |
| Dormant user deactivated but active again after a sync | UIDM-ISC still sends the account as active | Disable it in UIDM-ISC (notice "Dormant users deactivated") |
| User locked | `LOGIN_MAX_FAILED_ATTEMPTS` failed sign-ins (test accounts) | Users > Unlock |
