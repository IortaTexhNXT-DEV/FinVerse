package com.iortatechnxt.brokerverse.common.runtime;

/**
 * Kinds of work one BIBS instance can carry; a {@link RuntimeRole} selects which of them run.
 *
 * <ul>
 *   <li>{@link #USER_API}: screens and APIs for users ({@code /api/**}, API documentation).
 *   <li>{@link #BATCH}: scheduled and batch jobs (month-end, extractions, reports, reminders).
 *   <li>{@link #INTEGRATION}: outbox relay, Kafka consumers, inbound files from other systems and
 *       the {@code /integration/**} APIs published through the API gateway.
 * </ul>
 */
public enum Workload {
  /** Screens and APIs for users. */
  USER_API,
  /** Scheduled and batch jobs. */
  BATCH,
  /** Outbox relay, Kafka consumers, inbound files and the integration APIs. */
  INTEGRATION
}
