package com.iortatechnxt.brokerverse.audit.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the master change log (G05): the field-level history of one master record. */
@Service
@Transactional(readOnly = true)
public class MasterChangeLog {

  private static final int MAX_ROWS = 1000;

  private static final String HISTORY =
      "select id, entity_type, entity_id, change_kind, field_name, old_value, new_value, maker,"
          + " made_at, checker, checked_at from master_change_log"
          + " where entity_type = ? and entity_id = ? order by made_at desc, id desc limit ?";

  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC
   */
  public MasterChangeLog(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * One logged field change.
   *
   * @param id id
   * @param entityType record type
   * @param entityId record id
   * @param changeKind CREATE or UPDATE
   * @param field field name ("record" for a creation)
   * @param oldValue value before
   * @param newValue value after
   * @param maker user who made the change
   * @param madeAt time of the change
   * @param checker user who authorized it, null while pending or for records outside maker-checker
   * @param checkedAt time of the authorization
   */
  public record Entry(
      Long id,
      String entityType,
      Long entityId,
      String changeKind,
      String field,
      String oldValue,
      String newValue,
      String maker,
      Instant madeAt,
      String checker,
      Instant checkedAt) {}

  /**
   * The history of a record, latest first.
   *
   * @param entityType record type, e.g. InsurerProfile
   * @param entityId record id
   * @return entries
   */
  public List<Entry> history(String entityType, Long entityId) {
    return jdbc.query(HISTORY, (rs, row) -> entry(rs), entityType, entityId, MAX_ROWS);
  }

  private static Entry entry(ResultSet rs) throws SQLException {
    return new Entry(
        rs.getLong("id"),
        rs.getString("entity_type"),
        rs.getLong("entity_id"),
        rs.getString("change_kind"),
        rs.getString("field_name"),
        rs.getString("old_value"),
        rs.getString("new_value"),
        rs.getString("maker"),
        instant(rs.getTimestamp("made_at")),
        rs.getString("checker"),
        instant(rs.getTimestamp("checked_at")));
  }

  private static Instant instant(Timestamp t) {
    return t == null ? null : t.toInstant();
  }
}
