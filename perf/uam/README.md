# User Access load tests (k6)

Sizing: 530 named users, about 150 at once; UIDM-ISC full re-sync of 600 users and 2,000
group-profile membership changes. Scripts for [k6](https://k6.io) 0.54 or later.

| Script | What it does | Default load |
|---|---|---|
| `signin_menu.js` | Each virtual user signs in once, then opens screens: `/auth/me`, password status, session policy, role home, notification and approval counts, alerts (when allowed) | 150 VUs, 1 min ramp, 3 min hold, 5 s think |
| `users.js` | Users list, then one user: group profiles, access history, data access, sessions, directory profile | 30 VUs, 3 min |
| `group_profiles.js` | Group profiles, members, pending changes, access matrix, changes of one profile | 30 VUs, 3 min |
| `scim_burst.js` | `POST /Users` (409 -> `PUT /Users/{id}`) for 600 users, then 2,000 `PATCH /Groups/{id}` (one member each) | 10 VUs |
| `certification.js` | Certification extract CSV and XLSX, and its log | 5 VUs, 20 extracts |
| `mixed.js` | 120 users in the menu (1 min ramp), 15 on Users, 15 on Group Profiles, SCIM 10 changes/s | 3 min |

Thresholds (`lib/common.js`): errors < 1 %; reads p95 < 800 ms, p99 < 2 s; sign-in p95 < 1.5 s;
SCIM p95 < 1 s; extract p95 < 5 s.

SCIM has no `POST`/`PUT` on Groups: UIDM-ISC changes the membership with `PATCH /Groups/{id}`
(`add` / `remove` of members), and the group profiles themselves are kept in BrokerVerse.

## Run against a local backend

```bash
cd backend && mvn -DskipTests package                 # backend/target/brokerverse-backend.jar
createdb -O brokerverse uam_perf                       # throwaway database
perf/uam/local/run-backend.sh                          # seed profile on :18080, local SCIM key set
psql -d uam_perf -f perf/uam/local/group_profiles.sql  # 60 group profiles PERF_GP_01..60
cd perf/uam
SCIM_TOKEN=$(cat local/work/scim.token) k6 run scim_burst.js        # creates 600 users
PERF_USERS=$(psql -d uam_perf -Atc "select string_agg(username, ',') from sec_user
  where enabled and windows_id is null") k6 run signin_menu.js
k6 run users.js; k6 run group_profiles.js; k6 run certification.js
SCIM_TOKEN=$(cat local/work/scim.token) PERF_USERS=... k6 run mixed.js
```

`run-backend.sh` raises the sign-in and integration rate limits (one load generator, one address)
and signs a local gateway token (`local/integration_token.py`); never use it outside a local run.
Against SIT/UAT set `BASE_URL`, `PERF_PASSWORD`, `PERF_USERS` (test accounts) and `SCIM_TOKEN`
(a gateway token with `identity.provision`); keep the rate limits of the environment and spread
the virtual users over several addresses, or expect 429 above 20 sign-ins a minute per address.

## Results (local run, 16-Oct-2026)

Laptop-class host: 4 vCPU, PostgreSQL 16 on the same host, JVM `-Xmx1g`, pool 20 connections.
Data: 1,200 users (600 by SCIM + 600 new joiners), 157 group profiles, 120,000 sessions, 63,000
access change log rows.

| Run | Requests | Errors | p95 | p99 |
|---|---|---|---|---|
| `signin_menu.js` 150 VUs | 42,054 | 0 % | reads 14 ms, sign-in 373 ms | reads 24 ms, sign-in 389 ms |
| `users.js` 30 VUs | 6,289 | 0 % | 322 ms | 478 ms |
| `group_profiles.js` 30 VUs | 5,651 | 0 % | 112 ms | 330 ms |
| `certification.js` | 20 extracts | 0 % | 2.2 s (XLSX of 1,200 users) | 2.2 s |
| `scim_burst.js` re-sync of 600 + 2,000 changes | 3,200 | 0 % | 84 ms | 98 ms |
| `scim_burst.js` 600 new joiners | 600 | 0 % | 94 ms | 104 ms |
| `mixed.js` 150 users + SCIM | 35,368 | 0 % | reads 76 ms, SCIM 55 ms, sign-in 680 ms | reads 163 ms |

Before the fixes below the same runs gave: new joiners p95 1.12 s (600 in 57 s); XLSX extract
4.5 s; `mixed.js` 76 % errors (pool exhausted for 30 s by a sign-in burst).

## Query plans and fixes

Statements per request were counted with Hibernate statistics; plans with `EXPLAIN ANALYZE` on
the data above.

| Finding | Fix | Effect |
|---|---|---|
| N+1: the roles of each user and the permissions of each role loaded one owner at a time | `hibernate.default_batch_fetch_size: 100` (`application.yml`) | `/admin/users` 730 -> 12 statements, `/nbadmin/users` 730 -> 12, `/nbadmin/roles` 161 -> 6, access matrix 888 -> 15, SoD violations 731 -> 13, certification extract 742 -> 24 |
| `findBy...IgnoreCase` compares `upper(col) = upper(?)`; the indexes were on `lower(col)`: sequential scans of `sec_user`, `sec_user_session`, `sec_access_change_log` on every sign-in, request and SCIM call | V2833: `upper(...)` indexes on users, sessions, change log, second factor, password history and reset, directory profile, access requests | sessions of a user 33 ms -> 0.2 ms (index scan), access history 19 ms -> 0.6 ms, user by name 0.4 -> 0.1 ms |
| A joiner hashed a random password with BCrypt (300 ms of CPU each) | One unusable hash computed at start (`IdentityUserChanges`) | 600 joiners 57 s -> 4 s |
| Certification XLSX built in memory | Streaming workbook (`SXSSFWorkbook`, 500-row window) | 4.5 s -> 1.6 s, bounded memory |
| The password check (BCrypt) ran inside the sign-in transaction, holding a pool connection; a burst of sign-ins starved every request | Check outside any transaction (`AuthService`), at most one check per processor at once (`LocalPasswordAuthenticator`); an unknown user costs a check too (no timing clue) | `mixed.js` 76 % errors -> 0 % |
| Unbounded lists: `size` taken as given; SCIM `GET /Users` and `/Groups` returned everything | `PageSizeLimitFilter` (400 above `BROKERVERSE_MAX_PAGE_SIZE`, 500); SCIM `startIndex` / `count` at most 200 | bounded responses |

Open: `/admin/users` and `/nbadmin/users` still return every user (425 KB for 1,200 users; the
screen filters in the browser): fine at 530 users, to page on the server above a few thousand.
`GET /admin/identity/events` counts all events for its page total; partition or cap the count
once the event table passes a few million rows. Two sign-ins of the same account at the same
instant can answer 409 (optimistic lock on the user row).
