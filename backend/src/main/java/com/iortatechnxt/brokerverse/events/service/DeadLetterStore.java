package com.iortatechnxt.brokerverse.events.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** SQL access to {@code evt_dead_letter}. */
@Component
public class DeadLetterStore {

  private static final String INSERT =
      """
      insert into evt_dead_letter (event_id, original_topic, dlt_topic, kafka_partition,
          kafka_offset, event_key, consumer_group, error_message, payload, status, received_at)
      values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'NEW', ?)
      on conflict (dlt_topic, kafka_partition, kafka_offset) do nothing
      """;
  private static final String COLUMNS =
      "id, event_id, original_topic, dlt_topic, kafka_partition, kafka_offset, event_key,"
          + " consumer_group, error_message, payload, status, received_at, resolved_at,"
          + " resolved_by";
  private static final String FILTER = " where (cast(? as varchar) is null or status = ?)";
  private static final int MAX_ERROR = 2000;

  private final JdbcTemplate jdbc;

  /**
   * Creates the store.
   *
   * @param jdbc JDBC template
   */
  public DeadLetterStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Records a dead letter once (a redelivery of the same position is ignored).
   *
   * @param letter the dead letter
   */
  public void insert(DeadLetter letter) {
    String error = letter.errorMessage();
    jdbc.update(
        INSERT,
        letter.eventId(),
        letter.originalTopic(),
        letter.deadLetterTopic(),
        letter.partition(),
        letter.offset(),
        letter.key(),
        letter.consumerGroup(),
        error != null && error.length() > MAX_ERROR ? error.substring(0, MAX_ERROR) : error,
        letter.payload(),
        Timestamp.from(letter.receivedAt()));
  }

  /**
   * Dead letters, newest first.
   *
   * @param status status, null for all
   * @param limit page size
   * @param offset rows to skip
   * @return dead letters
   */
  public List<DeadLetter> search(String status, int limit, int offset) {
    return jdbc.query(
        "select "
            + COLUMNS
            + " from evt_dead_letter"
            + FILTER
            + " order by id desc limit ? offset ?",
        (rs, rowNum) -> map(rs),
        status,
        status,
        limit,
        offset);
  }

  /**
   * Number of dead letters of a status.
   *
   * @param status status, null for all
   * @return count
   */
  public long count(String status) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from evt_dead_letter" + FILTER, Long.class, status, status);
    return count == null ? 0 : count;
  }

  /**
   * One dead letter.
   *
   * @param id id
   * @return dead letter, empty when unknown
   */
  public Optional<DeadLetter> find(long id) {
    return jdbc
        .query(
            "select " + COLUMNS + " from evt_dead_letter where id = ?", (rs, rowNum) -> map(rs), id)
        .stream()
        .findFirst();
  }

  /**
   * Closes a NEW dead letter.
   *
   * @param id id
   * @param status RETRIED or DISCARDED
   * @param user support user
   * @param when time
   * @return true when it was NEW
   */
  public boolean resolve(long id, String status, String user, Instant when) {
    return jdbc.update(
            "update evt_dead_letter set status = ?, resolved_by = ?, resolved_at = ?"
                + " where id = ? and status = 'NEW'",
            status,
            user,
            Timestamp.from(when),
            id)
        > 0;
  }

  private static DeadLetter map(ResultSet rs) throws SQLException {
    Timestamp resolved = rs.getTimestamp("resolved_at");
    return new DeadLetter(
        rs.getLong("id"),
        rs.getObject("event_id", UUID.class),
        rs.getString("original_topic"),
        rs.getString("dlt_topic"),
        rs.getInt("kafka_partition"),
        rs.getLong("kafka_offset"),
        rs.getString("event_key"),
        rs.getString("consumer_group"),
        rs.getString("error_message"),
        rs.getString("payload"),
        rs.getString("status"),
        rs.getTimestamp("received_at").toInstant(),
        resolved == null ? null : resolved.toInstant(),
        rs.getString("resolved_by"));
  }

  /**
   * A record a consumer could not process.
   *
   * @param id row id (0 before insert)
   * @param eventId event id, null when the record was not an envelope
   * @param originalTopic topic it was consumed from
   * @param deadLetterTopic dead-letter topic
   * @param partition partition in the dead-letter topic
   * @param offset offset in the dead-letter topic
   * @param key record key
   * @param consumerGroup consumer group that failed
   * @param errorMessage last exception
   * @param payload record value as received
   * @param status NEW, RETRIED or DISCARDED
   * @param receivedAt time recorded
   * @param resolvedAt time retried or discarded
   * @param resolvedBy support user who retried or discarded it
   */
  public record DeadLetter(
      long id,
      UUID eventId,
      String originalTopic,
      String deadLetterTopic,
      int partition,
      long offset,
      String key,
      String consumerGroup,
      String errorMessage,
      String payload,
      String status,
      Instant receivedAt,
      Instant resolvedAt,
      String resolvedBy) {}
}
