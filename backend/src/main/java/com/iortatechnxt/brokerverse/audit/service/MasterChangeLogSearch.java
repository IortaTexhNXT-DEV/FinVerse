package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.common.security.DataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The programme-wide Master Data Change Log (G05): the field changes of every master record,
 * filtered by record type, date range, maker and checker, latest first, limited to the records the
 * reader's data scope holds (a record owned by a company or branch outside the scope is left out,
 * as is a record whose owner cannot be resolved).
 */
@Service
@Transactional(readOnly = true)
public class MasterChangeLogSearch {

  /** Most rows a search returns. */
  public static final int MAX_ROWS = 5000;

  private static final String TITLE = "Master Data Change Log";

  private static final String SEARCH =
      "select id, entity_type, entity_id, change_kind, field_name, old_value, new_value, maker,"
          + " made_at, checker, checked_at from master_change_log"
          + " where (cast(:type as text) is null or entity_type = :type)"
          + " and (cast(:from as timestamptz) is null or made_at >= :from)"
          + " and (cast(:to as timestamptz) is null or made_at < :to)"
          + " and (cast(:maker as text) is null or lower(maker) = lower(cast(:maker as text)))"
          + " and (cast(:checker as text) is null"
          + " or lower(checker) = lower(cast(:checker as text)))"
          + " order by made_at desc, id desc limit :limit";

  private final NamedParameterJdbcTemplate jdbc;
  private final RecordOwnership ownership;
  private final DataScope dataScope;

  /**
   * Creates the search.
   *
   * @param jdbc JDBC
   * @param ownership owner of a record
   * @param dataScope data scope of the reader
   */
  public MasterChangeLogSearch(
      NamedParameterJdbcTemplate jdbc, RecordOwnership ownership, DataScope dataScope) {
    this.jdbc = jdbc;
    this.ownership = ownership;
    this.dataScope = dataScope;
  }

  /**
   * The filters of the log; null values are not filtered on.
   *
   * @param entityType record type
   * @param from first day of the change
   * @param to last day of the change
   * @param maker user who made the change
   * @param checker user who authorized it
   */
  public record Filter(
      String entityType, LocalDate from, LocalDate to, String maker, String checker) {

    /**
     * The filters in words, for the title of the export.
     *
     * @return text
     */
    public String describe() {
      StringBuilder sb = new StringBuilder(TITLE);
      append(sb, "record type ", blankToNull(entityType));
      append(sb, "from ", from == null ? null : from.toString());
      append(sb, "to ", to == null ? null : to.toString());
      append(sb, "maker ", blankToNull(maker));
      append(sb, "checker ", blankToNull(checker));
      return sb.toString();
    }

    private static void append(StringBuilder sb, String label, String value) {
      if (value != null) {
        sb.append(", ").append(label).append(value);
      }
    }
  }

  /**
   * The changes of the filter within the reader's data scope, latest first.
   *
   * @param f filter
   * @return entries, at most {@value #MAX_ROWS}
   */
  public List<Entry> search(Filter f) {
    Map<String, Object> p = new HashMap<>();
    p.put("type", blankToNull(f.entityType()));
    p.put("from", f.from() == null ? null : Timestamp.from(BusinessClock.startOf(f.from())));
    p.put("to", f.to() == null ? null : Timestamp.from(BusinessClock.startOf(f.to().plusDays(1))));
    p.put("maker", blankToNull(f.maker()));
    p.put("checker", blankToNull(f.checker()));
    p.put("limit", MAX_ROWS);
    List<Entry> rows = jdbc.query(SEARCH, p, (rs, n) -> MasterChangeLog.entry(rs));
    UserDataScope scope = dataScope.allowed();
    if (scope.allCompanies()) {
      return rows;
    }
    Map<String, Boolean> visible = new HashMap<>();
    return rows.stream()
        .filter(
            e ->
                visible.computeIfAbsent(
                    e.entityType() + "#" + e.entityId(),
                    k ->
                        RecordOwnership.visible(
                            scope, ownership.ownerOf(e.entityType(), e.entityId()))))
        .toList();
  }

  /**
   * The record types present in the log.
   *
   * @return types, sorted
   */
  public List<String> entityTypes() {
    return jdbc.queryForList(
        "select distinct entity_type from master_change_log order by entity_type",
        Map.of(),
        String.class);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
