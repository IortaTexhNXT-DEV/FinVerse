package com.iortatechnxt.brokerverse.nbreport.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only SQL access of the New Business reports and dashboard. Statements are constants with
 * named bind parameters; optional filters use the {@code (cast(:x as varchar) is null or col = :x)}
 * idiom. Result values are normalised for the report renderers: SQL dates and timestamps become
 * {@link LocalDate} (timestamps in Philippine time), other values are kept.
 */
@Service
@Transactional(readOnly = true)
public class NbReportJdbc {

  /** Business time zone of BDOI (dates of timestamps). */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the executor.
   *
   * @param jdbc named-parameter JDBC template
   */
  public NbReportJdbc(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Runs a query and returns its rows with normalised values.
   *
   * @param sql constant SQL
   * @param params bind parameters (null values allowed)
   * @return rows as ordered maps of column label to value
   */
  public List<Map<String, Object>> rows(String sql, Map<String, ?> params) {
    return jdbc.queryForList(sql, source(params)).stream().map(NbReportJdbc::normalise).toList();
  }

  /**
   * Runs a count or sum query returning one number.
   *
   * @param sql constant SQL
   * @param params bind parameters
   * @return the value, zero when null
   */
  public long count(String sql, Map<String, ?> params) {
    Long value = jdbc.queryForObject(sql, source(params), Long.class);
    return value == null ? 0 : value;
  }

  private static MapSqlParameterSource source(Map<String, ?> params) {
    MapSqlParameterSource source = new MapSqlParameterSource();
    params.forEach(source::addValue);
    return source;
  }

  private static Map<String, Object> normalise(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    row.forEach((k, v) -> out.put(k, value(v)));
    return out;
  }

  private static Object value(Object v) {
    return switch (v) {
      case Date d -> d.toLocalDate();
      case Timestamp t -> t.toInstant().atZone(MANILA).toLocalDate();
      case null, default -> v;
    };
  }
}
