# Platform: Redis cache and shared state, Kafka integration events

BIBS technology baseline: **Redis 7.x** for caching and session state, **Apache Kafka 3.6+** for event
streaming and asynchronous integration. Everything else stays as it is (Java 21, Spring Boot 3.5,
PostgreSQL 16, React). This document is the design, the topic catalogue and the production support
runbook. Configuration: [`CONFIGURATION.md`](../operations/CONFIGURATION.md) (section *Redis 7 and
Apache Kafka*). How a module uses both: [Developer Guide §10.8 and §10.9](../development/DEVELOPER_GUIDE.md).

## 1. Overview

| Function | With Redis / Kafka | Fallback (switch off) | Code |
|---|---|---|---|
| Cluster-wide job lock | Redis `SET NX PX`, fencing token, lease renewed while the job runs | PostgreSQL advisory lock on a dedicated connection | `system.service.JobLock` → `sharedstate` |
| Reference-data cache | Spring Cache on Redis (JSON values, TTL per cache) | In-memory Caffeine cache, same TTL | `cache`, `CacheSpec` beans of the owners |
| Token denylist (logout) | Redis key per `jti`, TTL = remaining token life | Table `sec_revoked_token` | `security.service.TokenRevocationStore` → `sharedstate` |
| Failed-login counter, login rate limit | Redis `INCR` + `PEXPIRE` | Table `sys_shared_counter` (upsert) | `security.service.SharedCounterStore` → `sharedstate` |
| Integration events | Transactional outbox `evt_outbox` → relay → Kafka topics `bibs.<domain>.<event>.v1` | Rows recorded `LOCAL` (delivered in-process) | `events`, `integration` |
| E-mail dispatch | Consumer of `bibs.messaging.notification-requested.v1` | Delivery after commit + `MAIL_DISPATCH` job (unchanged) | `integration.service.NotificationDeliveryConsumer` |
| Event archive | Consumer → `evt_archive` | – (the outbox is the record) | `events.service.EventArchiveConsumer` |
| Dead letters | `<topic>.dlt` → `evt_dead_letter`, support screen with retry | – | `events.service.DeadLetterRecorder` |

Switches: `brokerverse.redis.enabled` and `brokerverse.kafka.enabled` (both `true` in `application.yml`,
`false` in the `test` profile). The application starts and behaves correctly with either off. All
instances of an environment must use the same values.

Packages (ArchUnit keeps them cycle-free: the platform owns the ports, the business modules never
depend on `sharedstate`, `events` or `integration`):

- `cache` – `CacheSpec`, cache manager (Redis or Caffeine), `CacheInvalidator` (entity-change eviction),
  cache support API.
- `sharedstate` – `RedisJobLock` / `AdvisoryJobLock`, `RedisSessionStore` / `JdbcSessionStore`,
  `SHARED_STATE_CLEANUP` job.
- `events` – `IntegrationEventPublisher` (outbox), `OutboxRelay` and the `EVENT_OUTBOX_RELAY` job, Kafka
  configuration and topics, archive consumer, dead-letter recorder, support API, `EVENT_HOUSEKEEPING`.
- `integration` – topic catalogue (`IntegrationTopics`), adapters from the modules' in-process events,
  entity-change capture (client, receipt), e-mail consumer.

Migration: **V28** `V28__platform_cache_and_events.sql` (platform range V1–V99): `sys_job_run.status`
`SKIPPED_LOCKED`, sequence `sys_job_lock_fence_seq`, tables `sec_revoked_token`, `sys_shared_counter`,
`evt_outbox`, `evt_archive`, `evt_dead_letter`. It depends only on V1/V20 and runs out of order on
databases that already have later versions.

## 2. Redis

### 2.1 Job lock

`JobRunService.execute` (every `ManagedJob` run, scheduled or "Run now", and every batch run started
from a screen) takes `JobLock.tryAcquire(jobName)` before the work:

- **taken** → the run is not executed; a `sys_job_run` row with status `SKIPPED_LOCKED` ("Skipped: the
  job is already running on another instance") is written and logged at INFO. With 2 replicas every
  cron fires on both pods: one run `SUCCEEDED`, one `SKIPPED_LOCKED`.
- **lock store unreachable** → the run is recorded `FAILED` ("Job lock unavailable") and `JOB_FAILURE`
  is raised; a job is never run unguarded.
- **Redis**: key `<prefix>joblock:<job>` = `<instance-uuid>:<fencing token>`, set with `SET NX PX
  <lease>` (`brokerverse.jobs.lock-lease`, 2 minutes). The fencing token is `INCR
  <prefix>joblock:<job>:fence` (strictly increasing per job). A daemon thread renews the lease every
  third of it with a compare-and-`PEXPIRE` script; release is a compare-and-`DEL` script, so an
  instance never extends or deletes a lock another instance took after its lease ran out. A crashed
  instance frees the lock after one lease.
- **PostgreSQL**: `pg_try_advisory_lock(0x4A4F42, hashtext(<job>))` on a dedicated pooled connection held
  for the run (session lock, released by `pg_advisory_unlock` or when the connection ends, so a dead
  pod never leaves a job locked). Fencing token: `nextval('sys_job_lock_fence_seq')`.

### 2.2 Reference-data cache

| Cache | Content and key | TTL | Cleared by (entity) | Read through |
|---|---|---|---|---|
| `lov-values` | every value of a list, key = list type | 1 h | `LovValue`, `LovType` | `LovService.label`, `validateOptional`, `options` (pick lists, `GET /lov/{type}/options`) via `LovLookup` |
| `system-parameters` | raw value, key = parameter key (null cached) | 15 min | `SystemParameter` | `SystemParameterService.intValue/text/items` via `SystemParameterLookup` |
| `security-role-permissions` | permissions of a role, key = role code | 15 min | `Role` (and its grants) | `AppUserDetailsService` (every request) via `RolePermissionLookup` |
| `catalog-product-versions` | version views `current:<p>:<date>`, `inForce:<p>:<date>`, `version:<p>:<n>`, `versions:<p>` | 1 h | `ProductVersion`, `RiskProduct` | `ProductVersionQueryService` (catalog `ProductVersionQueries`); **outside read-write transactions only** |
| `organization-units` | `company:<id>`, `company-code:<code>`, `branch:<id>`, `branches:<companyId>` | 1 h | `Company`, `Branch` | `OrganizationDirectory` (cached records; `OrganizationService` keeps returning entities) |

Eviction ("a change made by the System Administrator shows everywhere right away"):

1. **Every JPA write** of an entity listed in a `CacheSpec` (insert, update, delete, element-collection
   change, whatever the write path: the owning service, another module, a job, the bulk loader) clears
   the whole cache through a Hibernate listener (`CacheInvalidator`): once at the flush, and once more
   when the transaction completes (commit or rollback), so a value read inside the writing transaction
   never outlives it. With Redis the clear (`SCAN` + `DEL` of `<prefix>cache:<name>::*`) reaches every
   pod.
2. The maintenance services also carry `@CacheEvict` (LOV, parameters, roles, companies and branches),
   so a read later in the same request never sees the old value.
3. The catalog cache is bypassed inside read-write transactions (`WriteTransactionBypassCache`): the
   catalog has many write paths and a transaction that changes a product must read its own change.

Caches are whole-master caches (clear all on change), keys are the natural lookup keys; masters are
small and change rarely. Changes made outside JPA (SQL scripts, a manual fix) are not seen: flush the
cache (runbook §5.3). The time to live bounds any staleness, including the in-memory fallback on
several pods (each pod clears only its own memory; do not run several pods with Redis off in
production).

Values on Redis are JSON (`CacheValueSerializer`): `{"t": type, "v": value}`; only
`com.iortatechnxt.brokerverse.*` types and a short JDK allow-list are ever instantiated (no Java
serialization, no polymorphic typing), decimals keep their scale and unknown properties are ignored
(an entry written by the previous release still reads). Cache failures never fail a request
(`ResilientCacheErrorHandler`): a failed read is a miss, a failed eviction is logged as ERROR.

### 2.3 Session state

- **Logout** – `POST /api/v1/auth/logout` (bearer token; 204) revokes the token until its expiry and
  writes the audit entry `AppUser / LOGOUT` (UAM BRD-11, UAM-NFR-35). Every token carries a `jti`;
  `JwtAuthenticationFilter` refuses a token whose `jti` is on the denylist. Tokens issued before this
  release have no `jti` and are accepted until they expire. The web client calls logout when the user
  signs out. When the denylist cannot be read, the token is accepted and the failure logged (a Redis
  outage does not sign everybody out).
- **Failed logins** – `LoginAttemptTracker` counts consecutive failures on the shared counter
  `login-failures:<user>`; the count applied is `max(shared counter, recorded + 1)` and
  `sec_user.failed_attempts` stays the record (a user whose recorded count is 0 starts a fresh counter).
  `LOGIN_MAX_FAILED_ATTEMPTS` behaviour is unchanged; concurrent failures on two pods are both counted.
- **Rate limit** – `POST /api/v1/auth/login` is limited per client address (20 per minute by default,
  counted across pods): above it HTTP 429 with `Retry-After` and code `LOGIN_RATE_LIMITED`, before any
  password check. Counter store unreachable → request allowed (the lockout still protects).

Keys: `<prefix>session:revoked:<jti>`, `<prefix>counter:<key>`. Fallback tables are cleaned daily by
`SHARED_STATE_CLEANUP`.

## 3. Kafka

### 3.1 Transactional outbox

```
Business transaction (module)                     after commit                      Kafka
 ├─ business change                                ┌──────────────┐  in id order,   ┌─────────────┐
 ├─ Spring event ──► adapter (BEFORE_COMMIT) ──►   │ OutboxRelay  │  per key order  │ bibs.*.v1   │──► consumers
 │                   IntegrationEventPublisher     │ (job lock)   │ ───────────────►│ (+ .dlt)    │
 └─ INSERT evt_outbox (same transaction) ─────────►└──────────────┘  acks=all,      └─────────────┘
                                                   EVENT_OUTBOX_RELAY  idempotent
```

- `IntegrationEventPublisher.publish(IntegrationEvent)` inserts one `evt_outbox` row in the caller's
  transaction (a new one when none is active); the row commits or rolls back with the business change.
- After the commit the relay is woken (async); the `EVENT_OUTBOX_RELAY` job (every minute) sends
  whatever is left and the due retries. One relay runs at a time in the cluster (job lock of the same
  name) and reads `PENDING` rows in id order, so events of a key reach their partition in publication
  order. Producer: `acks=all`, `enable.idempotence=true`, `max.in.flight=5`.
- A batch stops at the first row the broker does not acknowledge: rows before it become `SENT`, that
  row waits (`relay-retry-backoff` doubling per attempt, max 1 h) and becomes `FAILED` after
  `relay-max-attempts`; rows after it are sent again later. Delivery is **at least once**; consumers
  deduplicate on `eventId`.
- In-process Spring events **stay** for logic in the same transaction (e.g. the Operations ledger feed,
  stage mirroring). Kafka carries facts **out** of the transaction for asynchronous work and
  integration.
- Kafka disabled: rows are written `LOCAL` (delivered in-process) at once; the job marks any leftover
  `PENDING` row `LOCAL`; nothing piles up.

### 3.2 Envelope (schema version 1)

Record key = `key`; headers `eventId`, `eventType`, `correlationId`; value (JSON):

```json
{
  "eventId": "7d1c1f0e-5c1a-4b5e-9f0e-2b8f7c0e9a11",
  "type": "booking.invoice.booked",
  "schemaVersion": 1,
  "occurredAt": "2026-09-25T08:15:30.123Z",
  "company": "FVI",
  "key": "BI-HO-2026-000123",
  "correlationId": "3f0a…",
  "source": "brokerverse",
  "payload": { "invoiceNo": "BI-HO-2026-000123", "…": "…" }
}
```

`correlationId` is the request's `X-Correlation-Id` (validated, or a new UUID; echoed in the response
and put in the log context by `CorrelationIdFilter`), so an event can be traced to the request or job
that caused it. A breaking payload change is a new topic version (`.v2`) published next to `.v1` until
the consumers have moved; additive fields do not change the version.

### 3.3 Topic catalogue

| Topic | Event types | Key | Source (adapter) | Payload |
|---|---|---|---|---|
| `bibs.booking.invoice-booked.v1` | `booking.invoice.booked` | invoice no. | `booking.service.InvoiceBooked` (`BookingEventAdapter`) | invoice, ARN, endorsement, kind, policy, client, shares, dates, risk / line, version, components, gross premium, commission, VAT, direct payment |
| `bibs.cashiering.payment-applied.v1` | `cashiering.payment.applied`, `cashiering.payment.unapplied` | invoice no. | `OpsLedgerEvents.InvoiceMovementPosted` (APPLIED / UNAPPLIED) (`OperationsEventAdapter`) | invoice, movement type, source module and reference (receipt / application), amount per component, total |
| `bibs.cashiering.receipt-issued.v1` | `cashiering.receipt.issued` | receipt no. | new `cashiering.domain.Receipt` (entity capture) | receipt id / no., kind, class, date, payor, currency, amount, mode, source |
| `bibs.remittance.batch-status.v1` | `remittance.batch.status-changed` | batch no. | `WorkCaseTransitioned` of `RemittanceBatch` (`WorkflowAndCatalogEventAdapter`) | batch, insurer, from / to stage, action, reason |
| `bibs.disbursement.status-changed.v1` | `disbursement.request.status-changed` | request no. | `OpsLedgerEvents.DisbursementStatusChanged` (`OperationsEventAdapter`) | request, type, source, status, DV no., DV stage, instrument status, reason |
| `bibs.collections.feed-ready.v1` | `collections.feed.ready` | feed code | `OpsLedgerEvents.CollectionFeedReady` (`OperationsEventAdapter`) | feed code |
| `bibs.catalog.product-version-released.v1` | `catalog.product-version.released` | product code | `ProductVersionReleased` (`WorkflowAndCatalogEventAdapter`) | product, version, effective from, request no., validator |
| `bibs.crm.client-changed.v1` | `crm.client.registered`, `crm.client.changed` | client id | `crm.domain.Client` insert / update (entity capture) | id, prospect / client code, type, display name, status, KYC status, segment, party, changed fields |
| `bibs.messaging.notification-requested.v1` | `messaging.notification.requested` | message id | `messaging.service.MessageQueuedEvent` (`MessagingEventAdapter`) | message id, purpose, record type / id, reference |

Every topic has a dead-letter topic `<topic>.dlt`. The adapters listen with
`@TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)`; the source modules are
unchanged. Where a module publishes no event (client, receipt), `EntityChangeCapture` (Hibernate
post-insert / post-update listener) writes the outbox row through the session's connection just before
the commit, after the final flush. Payloads carry identifiers, status and amounts, never personal data
(TIN, contact details, birth date) or e-mail content. Topics are created by the application (`KafkaAdmin`
`NewTopic`s, `brokerverse.kafka.partitions` / `replication-factor`); client and broker auto-creation stay
off.

### 3.4 Consumers

| Consumer group | Topics | Does | On failure |
|---|---|---|---|
| `bibs-mail-dispatch` | notification requested | `MailDispatcher.dispatch(messageId)` (one attempt; idempotent: only QUEUED messages) | a failed SMTP attempt stays QUEUED for `MAIL_DISPATCH`; a malformed record → DLT |
| `bibs-event-archive` | all topics | stores the envelope in `evt_archive` (unique `event_id`: redelivery ignored) | retries, then DLT |
| `bibs-dead-letter-recorder` | all `.dlt` topics | stores the record with original topic, consumer group and exception (`evt_dead_letter`, unique position) | logged and skipped after 2 retries (never dead-letters itself) |

Error handling: `brokerverse.kafka.consumer-retries` (3) retries `consumer-retry-backoff` (2 s) apart, then
the record goes unchanged, with the exception in its headers, to `<topic>.dlt`. Malformed envelopes
(`JsonProcessingException`, `InvalidEventException`) go to the DLT at once.

When Kafka is enabled, the messaging module no longer delivers right after commit: the consumer does it
asynchronously; `MAIL_DISPATCH` remains the safety net (retries, anything the consumer missed). When Kafka
is disabled the messaging module works exactly as before.

### 3.5 Support API and screen

*Administration › Integration Events* (permission `SYSTEM_PARAMETER_MANAGE`, System Administrator):

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/admin/events/topics` | catalogue with dead-letter topics |
| `GET /api/v1/admin/events/outbox?status=&topic=&key=&page=&size=` | outbox rows (PENDING, SENT, LOCAL, FAILED) |
| `POST /api/v1/admin/events/outbox/{id}/retry` | put a FAILED (or waiting) row back in the queue |
| `GET /api/v1/admin/events/archive?topic=&key=&correlationId=` | consumed events (traceability) |
| `GET /api/v1/admin/events/dead-letters?status=NEW\|RETRIED\|DISCARDED\|ALL` | dead letters |
| `POST /api/v1/admin/events/dead-letters/{id}/retry` | publish the stored record again to its original topic (every group of that topic sees it again and deduplicates) |
| `POST /api/v1/admin/events/dead-letters/{id}/discard` | close it without processing |
| `GET /api/v1/admin/caches`, `POST /api/v1/admin/caches/{name}/clear`, `POST /api/v1/admin/caches/clear` | list and flush the caches |

## 4. What happens when …

| Situation | Effect |
|---|---|
| `brokerverse.redis.enabled=false` | In-memory caches per pod (TTL-bounded staleness across pods), advisory job locks, denylist and counters in PostgreSQL. Correct on one pod; on several pods only the caches may lag by at most their TTL. |
| Redis unreachable (enabled) | Cache reads fall back to the database (logged); job runs are recorded FAILED "Job lock unavailable" (JOB_FAILURE alert) and not executed; the denylist check lets tokens through (logged); failed-login counting uses the database count; the login rate limit lets requests through; logout fails with 500 (the client still signs out locally). `/actuator/health` reports Redis DOWN. |
| `brokerverse.kafka.enabled=false` | Outbox rows written `LOCAL`; no consumers; e-mail delivered after commit and by `MAIL_DISPATCH`; the support API refuses retries (`KAFKA_DISABLED`). |
| Kafka unreachable (enabled) | Business transactions are not affected (they only write the outbox). Rows stay `PENDING` and are retried with back-off, `FAILED` after 10 attempts. E-mails wait (the `MAIL_DISPATCH` job still sends QUEUED mail). |
| A consumer keeps failing | After the retries the record is in `<topic>.dlt` and on the support screen. |
| A pod dies during a job | Redis lease expires (≤ 2 min) or the advisory lock goes with the connection; the next run proceeds. |

## 5. Runbook (production support)

### 5.1 Inspecting the outbox

- Screen *Administration › Integration Events › Outbox*, or SQL:
  `select status, count(*) from evt_outbox group by status;`
  `select id, topic, event_key, attempts, last_error, next_attempt_at from evt_outbox where status in ('PENDING','FAILED') order by id;`
- Many `PENDING` rows with `attempts > 0`: the broker is unreachable or refuses the producer (check
  `last_error`, MSK health, security groups, SASL secret). They are sent automatically once Kafka is
  back; the `EVENT_OUTBOX_RELAY` job history shows each round.
- `FAILED` rows: after the cause is fixed, *Send Again* (or `POST /admin/events/outbox/{id}/retry`).
  Bulk: `update evt_outbox set status='PENDING', attempts=0, next_attempt_at=now() where status='FAILED';`
- Trace a business record: `select * from evt_outbox where event_key = 'BI-HO-2026-000123';` then
  `evt_archive` by `event_id` or `correlation_id` (also in the application log of the request).

### 5.2 Replaying dead-letter events

1. *Integration Events › Dead Letters*: read the error and the consumer group.
2. Fix the cause (data, configuration, a code fix deployed).
3. *Retry*: the record is published again, unchanged, to its original topic. Consumers that already
   processed it ignore it (event id); the failing one processes it. If it fails again it comes back as
   a new dead letter.
4. *Discard* what must not be processed (documented in the incident).
5. Re-publishing without the screen: `kafka-console-producer.sh --topic <original topic> --property
   parse.key=true` with the stored `payload` (never needed in normal operation).

To replay a whole topic for a consumer (e.g. rebuild `evt_archive`), stop the application pods, reset
the consumer group offsets with `kafka-consumer-groups.sh --group bibs-event-archive --reset-offsets
--to-datetime … --topic … --execute`, then start the pods (idempotent consumers).

### 5.3 Flushing caches

- After a change made outside the application (SQL fix of `sys_parameter`, `lov_value`, `sec_role_permission`,
  `org_company`, catalog tables): `POST /api/v1/admin/caches/{name}/clear` (e.g. `system-parameters`) or
  `POST /api/v1/admin/caches/clear` as a System Administrator.
- Directly on Redis: `redis-cli --tls -h <endpoint> -a <token> --scan --pattern 'bv:cache:system-parameters::*' | xargs redis-cli … del`.
  Never `FLUSHALL`: it would also drop the job locks and the token denylist.
- With Redis disabled each pod has its own memory cache: call the endpoint once per pod or wait for the TTL.

### 5.4 Job locks

- A job shows `SKIPPED_LOCKED` on one pod and `SUCCEEDED` on the other: normal.
- A job skipped on every pod for longer than its run time: a stale lock. Redis:
  `GET bv:joblock:<JOB>` (owner `<instance>:<token>`), `PTTL` shows the remaining lease; it expires by
  itself within the lease; `DEL bv:joblock:<JOB>` only when the owner instance is gone. PostgreSQL:
  `select * from pg_locks where locktype='advisory' and classid=4869954;` then terminate the holding
  backend (`pg_terminate_backend(pid)`) only if its pod is gone.

### 5.5 Revoking sessions

A single token is revoked by the user's logout. To sign everybody out rotate `BROKERVERSE_JWT_SECRET`
(rolling restart). Revoked tokens: Redis `bv:session:revoked:*`, fallback table `sec_revoked_token`.

## 6. Tests (no Docker)

- Redis adapters: pure-Java Redis protocol server `com.github.fppt:jedis-mock` (Maven Central, runs Lua
  scripts): `RedisJobLockTest`, `RedisSessionStoreTest`, `RedisCacheTest`; the same behaviour tests
  (`JobLockContract`, `SessionStoreContract`) run on the PostgreSQL fallbacks (`AdvisoryJobLockIT`,
  `JdbcSessionStoreIT`).
- Kafka: `spring-kafka-test` `@EmbeddedKafka` (KRaft): `KafkaEventsIT` covers outbox → topic → consumers
  (archive, e-mail), order per key, dead-letter topic, retry and discard.
- The whole application with Redis and Kafka on: `PlatformServicesSupport` (jedis-mock + embedded
  Kafka): `RedisEnabledPlatformIT` (job lock `SKIPPED_LOCKED`, cache eviction on Redis, logout
  revocation, shared failed-login counter) and `KafkaEventsIT`.
- Fallbacks: `ReferenceDataCacheIT` (a write evicts), `LogoutIT`, `IntegrationEventsLocalIT` (every
  adapter, rollback publishes nothing, LOCAL, support API). The `test` profile needs neither Redis nor
  Kafka.
