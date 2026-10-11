# iNXT BrokerVerse - Company and Branch Data Scope (platform capability)

Status: **built**. Owner: platform team (`security`). Flyway: schema **V1240**, range V1240-V1249 (seed range V2040-V2049, not used).

## 1. Problem

iNXT BrokerVerse is multi-company. About 194 controllers accept a `companyId` (query parameter, path variable or request body) and a few accept a branch. Before this change nothing central checked that the signed-in user may act for the company or branch a request names: a user of company A could read or change the data of company B by changing one parameter. The permission model (roles and permissions) answers *what* a user may do, not *for which company*.

## 2. Model

Each user has a **data scope**:

| Level | Values | Meaning |
|---|---|---|
| Companies | **All companies** (default) or a list of companies | The companies whose data the user may read and change |
| Branches, per listed company | **All branches** (default) or a list of branches of that company | The branches of the company the user may act for |

Storage (V1240):

- `sec_user.all_companies` (boolean, not null, **default true**). True = all companies, present and future.
- `sec_user_data_scope` - one row per granted company (`branch_id` null = all branches of the company) or per granted branch (`branch_id` set). Foreign keys to `sec_user`, `org_company` and `org_branch`; a company is granted either with all its branches or with a list of branches, never both (service rule, partial unique indexes).

The scope travels as one value, `UserDataScope` (`common.security`): `allCompanies` and a list of `CompanyScope(companyId, allBranches, branchIds)`. Its text form (`ALL`, or `12:*;14:3,5`) is what an access request stores and what the access change log records.

### 2.1 Defaults and how existing data stays visible

- **Backfill:** the column default sets `all_companies = true` for every existing user. No scope row is written. Every existing user therefore keeps access to every company and branch.
- **New users:** every user created afterwards (Users screen, access request, external user provisioning, bulk upload, seed data, migration loads) gets the same default from the entity and the column default.
- The behaviour of a current deployment therefore **does not change** until an administrator narrows a user's scope. Narrowing is an explicit, audited change (section 5).

### 2.2 Fail closed

The guard refuses (HTTP 403, code `DATA_SCOPE_DENIED`, business message "You do not have access to the data of this company" / "... of this branch") when:

- the signed-in user is unknown (no `sec_user` row),
- the user's scope does not list the company, or lists the company with a branch list that does not hold the branch,
- a user request runs without a signed-in user,
- the user is restricted (not "All companies") and has no scope rows at all (a scope that grants nothing).

## 3. Guard API

The contract is the interface `common.security.DataScope`, so any module can call it without a new module dependency (every module already depends on `common`). The implementation is `security.service.DataScopeGuard`.

| Method | Use |
|---|---|
| `requireCompany(Long companyId)` | Refuses when the company is outside the user's scope. Null is not checked (an optional filter that is absent). |
| `requireBranch(Long companyId, Long branchId)` | Refuses when the branch is outside the scope. With a null company the branch's company is looked up. |
| `allowed()` | The allowed sets for list queries, as a `UserDataScope`: `allCompanies`, or the allowed company ids (`companyIds()`) and, per company, all or the listed branch ids (`allowsCompany`, `allowsBranch`). |
| `filter(List<T>, Function<T, Long> companyOf)` | Keeps the rows of allowed companies (rows without a company stay). |

Resolution is cached per user (`security-data-scope`, 15 minutes, platform cache conventions of `PLATFORM_CACHE_AND_EVENTS.md`). Any change of `AppUser` or `UserDataScopeGrant` clears the cache on every instance; the scope service also evicts it explicitly.

## 4. Wiring

One mechanism per way a company reaches a controller, instead of 194 hand edits:

| Where the company is | Mechanism | Class |
|---|---|---|
| Query parameter `companyId` | `HandlerInterceptor` on `/api/**`, before the controller | `security.api.DataScopeInterceptor` |
| Path variable `{companyId}` | same interceptor (URI template variables) | same |
| Query parameter `branchId` (company branches) | same interceptor: `requireBranch(companyId, branchId)` | same |
| Request body with a `companyId` (and `branchId`) accessor, a list of such items, or a record body with a list component of such items (bulk requests) | `RequestBodyAdvice` after the body is read | `security.api.DataScopeBodyAdvice` (rules in `DataScopeTargets`) |
| Anything else (a company under another name; a multipart part; a header; a parameter bound under another name) | explicit `dataScope.requireCompany(...)` in the method, marked `@CompanyScoped` | `common.security.CompanyScoped` |

Path variables named `branchId` are not checked as company branches: the insurer branch maintenance (`/catalog/insurers/branches/{branchId}`) uses that name for the branches of an insurer.

**List and search endpoints** that return several companies' data filter on `allowed()` in the service, never only on the screen:

- company list and branch list of a company (`OrganizationService.listCompaniesInScope`, `listBranchesInScope`; the company and branch pickers of every screen; jobs keep using the unfiltered `listCompanies`),
- approval inbox and counts without a company filter (`ApprovalInboxService`).

**Enforcement.** `ArchitectureTest.companyEndpointsAreDataScoped` fails the build when a controller method takes a company that none of the mechanisms reads and the method is not `@CompanyScoped`. `DataScopeCoverageIT` walks every request mapping of the running application, lists the endpoints that take a company and proves each one is covered (the interceptor is in the handler chain of its path, the body advice reads its body, or the method is `@CompanyScoped`) or exempt; it writes the figures to `target/data-scope-coverage.txt`.

Coverage at delivery: 1,965 request mappings, **747 take a company, 747 guarded** (560 by the interceptor, 183 by the body advice, 4 explicitly); none under `/integration/**` takes a company.

## 5. Administration and audit

- **Who changes a scope.** User changes go through the access request flow (maker-checker): a *Create user* or *Modify user* request carries the requested data scope; on approval `AccessChangeApplier` applies it with the request number and the approver. The emergency direct edit of the Users screen (`USER_MANAGE`, shown only when `UAM_DIRECT_ROLE_EDIT` is on) can also change it, as it can change the other user data.
- **No escalation.** An administrator, a requester and an approver can grant only companies and branches inside their own scope; nobody changes his own scope.
- **Audit.** Every change writes the access change log (`sec_access_change_log`, activity `DATA_SCOPE_CHANGED`, attribute `dataScope`, from / to in text form, request number and approver) and the audit trail, in the same transaction.
- **Screen.** User Maintenance (Users) shows a *Data Access* column and, in the row action menu, a *Data access* dialog per user: a tree table of companies and their branches with "All companies" and "All branches" as the default. It is editable only in the emergency direct edit and read-only otherwise, with *Raise Request* to change it. The *Enrol new user* and *Modify user* requests carry an editable *Data Access* section (current data access shown on a modification); the request details show the requested data access read-only to the approver.

API (`/api/v1/admin`): `GET /data-scope/units` (companies and branches the viewer may grant), `GET /users/{id}/data-scope`, `PUT /users/{id}/data-scope` (direct change, `USER_MANAGE`).

## 6. Exemptions

Explicit, in `DataScopeGuard.systemProcessing()` and in the interceptor registration:

| Exempt | Why | How |
|---|---|---|
| Background processing: scheduled jobs (`ManagedJob`), event consumers, outbox relay, seed and migration loaders | They run as the system for every company | Not an HTTP request dispatched to a controller (no request on the thread, or no handler chosen for it): the guard answers "all" |
| System-to-system APIs `/integration/**` | Gateway-authenticated systems, own security chain | The interceptor is registered on `/api/**` only; the guard answers "all" for a request under `/integration/` |
| Sign-in, session and profile endpoints (`/api/v1/auth/**`) | Take no company | Not matched (no `companyId`) |

A user who starts a job from a screen ("Run now") is still checked on the request that starts it.

## 7. Risks and limits

- **Records addressed by id only.** An endpoint that loads a record by its id without a `companyId` (for example `GET /claims/{id}`) is not covered by this capability: the record carries its company but the request does not name it. Closing this needs a per-module check on load (follow-up per module, using `requireCompany(record.companyId())`).
- **Reports and exports** that take a company are covered by the interceptor; whether branch scope must also filter the lines of a company-wide report is a business decision (section 8).
- **Home branch.** The user's home branch is not forced into the scope; an administrator who narrows the branches should keep it.
- **Cache staleness.** A narrowed scope applies within one request on the instance that made the change and at once on the others (cache cleared through Valkey); a user's open screens show refusals on the next call.
- **Performance.** One cache read per request that names a company; no database read while the entry is cached.

## 8. Business decisions for the client

1. Who may narrow a scope: any holder of the user maintenance permission with an approved request (as built), or a dedicated permission.
2. Whether branch scope also filters the lines of company-wide reports and inquiries (as built it applies only where a request names a branch).
3. Whether a restricted user's home branch must always be in the scope.
4. Whether a scope change needs a second approval (risk flag) as a privileged change does.
