package com.iortatechnxt.brokerverse.events.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/** SQL access to {@code evt_outbox}. */
@Component
public class OutboxStore {

  private static final String INSERT =
      """
      insert into evt_outbox (event_id, topic, event_type, event_key, company_id, company_code,
          correlation_id, occurred_at, payload, status, attempts, next_attempt_at, created_at,
          created_by)
      values (?, ?, ?, ?, ?, (select code from org_company where id = ?), ?, ?, ?, ?, 0, ?, ?, ?)
      """;
  private static final String COLUMNS =
      "id, event_id, topic, event_type, event_key, company_id, company_code, correlation_id,"
          + " occurred_at, payload, status, attempts, next_attempt_at, last_error, sent_at";
  private static final String DUE =
      "select "
          + COLUMNS
          + " from evt_outbox where status = 'PENDING' and next_attempt_at <= ? order by id limit ?";
  private static final String BY_ID = "select " + COLUMNS + " from evt_outbox where id = ?";
  private static final String SEARCH =
      "select "
          + COLUMNS
          + " from evt_outbox where (cast(? as varchar) is null or status = ?)"
          + " and (cast(? as varchar) is null or topic = ?)"
          + " and (cast(? as varchar) is null or event_key = ?)"
          + " order by id desc limit ? offset ?";
  private static final String COUNT =
      "select count(*) from evt_outbox where (cast(? as varchar) is null or status = ?)"
          + " and (cast(? as varchar) is null or topic = ?)"
          + " and (cast(? as varchar) is null or event_key = ?)";
  private static final RowMapper<OutboxEntry> ROW = (rs, rowNum) -> map(rs);

  private final JdbcTemplate jdbc;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC template (joins the current transaction)
   */
  public OutboxStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Inserts an event in the current transaction.
   *
   * @param row the row
   */
  public void insert(NewOutboxRow row) {
    insert(jdbc, row);
  }

  /**
   * Inserts an event through the given JDBC access (a Hibernate session connection).
   *
   * @param operations JDBC access
   * @param row the row
   */
  public void insert(JdbcOperations operations, NewOutboxRow row) {
    Timestamp occurred = Timestamp.from(row.occurredAt());
    operations.update(
        INSERT,
        row.eventId(),
        row.topic(),
        row.type(),
        row.key(),
        row.companyId(),
        row.companyId(),
        row.correlationId(),
        occurred,
        row.payload(),
        row.status().name(),
        occurred,
        occurred,
        row.createdBy());
  }

  /**
   * Pending rows due for sending, in publication order.
   *
   * @param now current time
   * @param limit maximum rows
   * @return rows ordered by id
   */
  public List<OutboxEntry> due(Instant now, int limit) {
    return jdbc.query(DUE, ROW, Timestamp.from(now), limit);
  }

  /**
   * One row.
   *
   * @param id row id
   * @return row, empty when unknown
   */
  public Optional<OutboxEntry> find(long id) {
    return jdbc.query(BY_ID, ROW, id).stream().findFirst();
  }

  /**
   * Rows for the support screen, newest first.
   *
   * @param filter optional status, topic and key
   * @param limit page size
   * @param offset rows to skip
   * @return rows
   */
  public List<OutboxEntry> search(OutboxFilter filter, int limit, int offset) {
    return jdbc.query(
        SEARCH,
        ROW,
        filter.status(),
        filter.status(),
        filter.topic(),
        filter.topic(),
        filter.key(),
        filter.key(),
        limit,
        offset);
  }

  /**
   * Number of rows matching a filter.
   *
   * @param filter optional status, topic and key
   * @return count
   */
  public long count(OutboxFilter filter) {
    Long count =
        jdbc.queryForObject(
            COUNT,
            Long.class,
            filter.status(),
            filter.status(),
            filter.topic(),
            filter.topic(),
            filter.key(),
            filter.key());
    return count == null ? 0 : count;
  }

  /**
   * Marks rows sent to Kafka.
   *
   * @param ids row ids
   * @param when acknowledgement time
   */
  public void markSent(List<Long> ids, Instant when) {
    Timestamp at = Timestamp.from(when);
    jdbc.batchUpdate(
        "update evt_outbox set status = 'SENT', sent_at = ?, last_error = null where id = ?",
        ids.stream().map(id -> new Object[] {at, id}).toList());
  }

  /**
   * Marks every pending row delivered in-process (Kafka disabled).
   *
   * @param when time
   * @return rows changed
   */
  public int markPendingLocal(Instant when) {
    return jdbc.update(
        "update evt_outbox set status = 'LOCAL', sent_at = ? where status = 'PENDING'",
        Timestamp.from(when));
  }

  /**
   * Records a failed send: the row waits for its next attempt, or becomes FAILED.
   *
   * @param id row id
   * @param error error summary
   * @param nextAttempt next attempt time, null to mark the row FAILED
   */
  public void markAttemptFailed(long id, String error, Instant nextAttempt) {
    if (nextAttempt == null) {
      jdbc.update(
          "update evt_outbox set status = 'FAILED', attempts = attempts + 1, last_error = ?"
              + " where id = ?",
          error,
          id);
    } else {
      jdbc.update(
          "update evt_outbox set attempts = attempts + 1, last_error = ?, next_attempt_at = ?"
              + " where id = ?",
          error,
          Timestamp.from(nextAttempt),
          id);
    }
  }

  /**
   * Puts a FAILED (or waiting) row back in the queue for an immediate send.
   *
   * @param id row id
   * @param when time
   * @return true when the row was requeued
   */
  public boolean requeue(long id, Instant when) {
    return jdbc.update(
            "update evt_outbox set status = 'PENDING', attempts = 0, next_attempt_at = ?"
                + " where id = ? and status in ('FAILED', 'PENDING')",
            Timestamp.from(when),
            id)
        > 0;
  }

  /**
   * Deletes delivered rows older than a time.
   *
   * @param before cut-off
   * @return rows deleted
   */
  public int purgeDelivered(Instant before) {
    return jdbc.update(
        "delete from evt_outbox where status in ('SENT', 'LOCAL') and created_at < ?",
        Timestamp.from(before));
  }

  private static OutboxEntry map(ResultSet rs) throws SQLException {
    Timestamp sent = rs.getTimestamp("sent_at");
    return new OutboxEntry(
        rs.getLong("id"),
        rs.getObject("event_id", UUID.class),
        rs.getString("topic"),
        rs.getString("event_type"),
        rs.getString("event_key"),
        rs.getObject("company_id", Long.class),
        rs.getString("company_code"),
        rs.getString("correlation_id"),
        rs.getTimestamp("occurred_at").toInstant(),
        rs.getString("payload"),
        OutboxStatus.valueOf(rs.getString("status")),
        rs.getInt("attempts"),
        rs.getTimestamp("next_attempt_at").toInstant(),
        rs.getString("last_error"),
        sent == null ? null : sent.toInstant());
  }

  /** Status of an outbox row. */
  public enum OutboxStatus {
    /** Waiting to be sent to Kafka. */
    PENDING,
    /** Acknowledged by Kafka. */
    SENT,
    /** Delivered in-process: Kafka is disabled. */
    LOCAL,
    /** Send attempts exhausted; a support user can requeue it. */
    FAILED
  }

  /**
   * A row to insert.
   *
   * @param eventId event id
   * @param topic topic
   * @param type event type
   * @param key ordering key
   * @param companyId company, may be null (the code is looked up)
   * @param correlationId correlation id
   * @param occurredAt event time
   * @param payload JSON payload
   * @param status PENDING or LOCAL
   * @param createdBy user or SYSTEM
   */
  public record NewOutboxRow(
      UUID eventId,
      String topic,
      String type,
      String key,
      Long companyId,
      String correlationId,
      Instant occurredAt,
      String payload,
      OutboxStatus status,
      String createdBy) {}

  /**
   * Filter of the support screen.
   *
   * @param status status name, null for all
   * @param topic topic, null for all
   * @param key event key, null for all
   */
  public record OutboxFilter(String status, String topic, String key) {}

  /**
   * One outbox row.
   *
   * @param id row id
   * @param eventId event id
   * @param topic topic
   * @param type event type
   * @param key ordering key
   * @param companyId company id
   * @param companyCode company code
   * @param correlationId correlation id
   * @param occurredAt event time
   * @param payload JSON payload
   * @param status status
   * @param attempts failed send attempts
   * @param nextAttemptAt next send attempt
   * @param lastError last send error
   * @param sentAt acknowledgement or local delivery time
   */
  public record OutboxEntry(
      long id,
      UUID eventId,
      String topic,
      String type,
      String key,
      Long companyId,
      String companyCode,
      String correlationId,
      Instant occurredAt,
      String payload,
      OutboxStatus status,
      int attempts,
      Instant nextAttemptAt,
      String lastError,
      Instant sentAt) {}
}
