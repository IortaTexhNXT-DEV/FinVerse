package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.DataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
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
  private final RecordOwnership ownership;
  private final DataScope dataScope;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC
   * @param ownership owner of a logged record
   * @param dataScope data scope of the signed-in user
   */
  public MasterChangeLog(JdbcTemplate jdbc, RecordOwnership ownership, DataScope dataScope) {
    this.jdbc = jdbc;
    this.ownership = ownership;
    this.dataScope = dataScope;
  }

  /**
   * The history of a record for the signed-in user: refused as not found when the record belongs to
   * a company or branch outside the user's data scope, or, for a user with a narrowed scope, when
   * its owner cannot be resolved (an unknown type or a removed record).
   *
   * @param entityType record type
   * @param entityId record id
   * @return entries, latest first
   * @throws ResourceNotFoundException when the record is outside the data scope
   */
  public List<Entry> scopedHistory(String entityType, Long entityId) {
    UserDataScope scope = dataScope.allowed();
    if (!scope.allCompanies()
        && !RecordOwnership.visible(scope, ownership.ownerOf(entityType, entityId))) {
      throw new ResourceNotFoundException("change history", entityType + " " + entityId);
    }
    return history(entityType, entityId);
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

  static Entry entry(ResultSet rs) throws SQLException {
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
