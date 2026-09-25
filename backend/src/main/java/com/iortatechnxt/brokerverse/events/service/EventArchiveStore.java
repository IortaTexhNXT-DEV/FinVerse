package com.iortatechnxt.brokerverse.events.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** SQL access to {@code evt_archive}. */
@Component
public class EventArchiveStore {

  private static final String INSERT =
      """
      insert into evt_archive (event_id, topic, event_type, event_key, company_code,
          correlation_id, occurred_at, kafka_partition, kafka_offset, envelope, archived_at)
      values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      on conflict (event_id) do nothing
      """;
  private static final String FILTER =
      " where (cast(? as varchar) is null or topic = ?)"
          + " and (cast(? as varchar) is null or event_key = ?)"
          + " and (cast(? as varchar) is null or correlation_id = ?)";
  private static final String SEARCH =
      "select id, event_id, topic, event_type, event_key, company_code, correlation_id,"
          + " occurred_at, kafka_partition, kafka_offset, envelope, archived_at from evt_archive"
          + FILTER
          + " order by id desc limit ? offset ?";
  private static final String COUNT = "select count(*) from evt_archive" + FILTER;

  private final JdbcTemplate jdbc;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC template
   */
  public EventArchiveStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Archives a consumed event once (a redelivery is ignored).
   *
   * @param envelope envelope
   * @param topic topic
   * @param partition partition
   * @param offset offset
   * @param json envelope JSON as received
   * @param when archive time
   * @return true when stored, false for a duplicate
   */
  public boolean insert(
      EventEnvelope envelope, String topic, int partition, long offset, String json, Instant when) {
    return jdbc.update(
            INSERT,
            envelope.eventId(),
            topic,
            envelope.type(),
            envelope.key() == null ? "" : envelope.key(),
            envelope.company(),
            envelope.correlationId(),
            Timestamp.from(envelope.occurredAt()),
            partition,
            offset,
            json,
            Timestamp.from(when))
        > 0;
  }

  /**
   * Archived events, newest first.
   *
   * @param filter optional topic, key and correlation id
   * @param limit page size
   * @param offset rows to skip
   * @return events
   */
  public List<ArchivedEvent> search(ArchiveFilter filter, int limit, int offset) {
    return jdbc.query(SEARCH, (rs, rowNum) -> map(rs), args(filter, limit, offset, true).toArray());
  }

  /**
   * Number of archived events matching a filter.
   *
   * @param filter optional topic, key and correlation id
   * @return count
   */
  public long count(ArchiveFilter filter) {
    Long count = jdbc.queryForObject(COUNT, Long.class, args(filter, 0, 0, false).toArray());
    return count == null ? 0 : count;
  }

  private static List<Object> args(ArchiveFilter f, int limit, int offset, boolean paged) {
    List<Object> args =
        new ArrayList<>(
            Arrays.asList(
                f.topic(), f.topic(), f.key(), f.key(), f.correlationId(), f.correlationId()));
    if (paged) {
      args.add(limit);
      args.add(offset);
    }
    return args;
  }

  private static ArchivedEvent map(ResultSet rs) throws SQLException {
    return new ArchivedEvent(
        rs.getLong("id"),
        rs.getObject("event_id", UUID.class),
        rs.getString("topic"),
        rs.getString("event_type"),
        rs.getString("event_key"),
        rs.getString("company_code"),
        rs.getString("correlation_id"),
        rs.getTimestamp("occurred_at").toInstant(),
        rs.getInt("kafka_partition"),
        rs.getLong("kafka_offset"),
        rs.getString("envelope"),
        rs.getTimestamp("archived_at").toInstant());
  }

  /**
   * Filter of the archive search.
   *
   * @param topic topic, null for all
   * @param key event key, null for all
   * @param correlationId correlation id, null for all
   */
  public record ArchiveFilter(String topic, String key, String correlationId) {}

  /**
   * One archived event.
   *
   * @param id row id
   * @param eventId event id
   * @param topic topic
   * @param type event type
   * @param key event key
   * @param companyCode company code
   * @param correlationId correlation id
   * @param occurredAt event time
   * @param partition partition
   * @param offset offset
   * @param envelope envelope JSON
   * @param archivedAt archive time
   */
  public record ArchivedEvent(
      long id,
      UUID eventId,
      String topic,
      String type,
      String key,
      String companyCode,
      String correlationId,
      Instant occurredAt,
      int partition,
      long offset,
      String envelope,
      Instant archivedAt) {}
}
