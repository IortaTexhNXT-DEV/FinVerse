package com.iortatechnxt.brokerverse.configpromo.engine;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Values of a package: booleans stay booleans, every other value is text in one form per type
 * (numbers without exponent, dates yyyy-MM-dd, timestamps in ISO-8601 UTC), so the same value reads
 * the same in every environment. Written back with a cast to the type of the target column.
 */
public final class CanonicalValues {

  private static final Set<String> TEXT_TYPES = Set.of("varchar", "text", "bpchar");
  private static final Set<String> INTEGER_TYPES = Set.of("int2", "int4", "int8");

  private CanonicalValues() {}

  /**
   * Reads a column of the current row.
   *
   * @param rs result set
   * @param column column label
   * @param type PostgreSQL type of the column
   * @return canonical value or null
   * @throws SQLException when the column cannot be read
   */
  public static Object read(ResultSet rs, String column, String type) throws SQLException {
    switch (type) {
      case "bool" -> {
        boolean v = rs.getBoolean(column);
        return rs.wasNull() ? null : v;
      }
      case "date" -> {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate().toString();
      }
      case "timestamptz" -> {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant().toString();
      }
      case "timestamp" -> {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime().toString();
      }
      case "numeric" -> {
        BigDecimal n = rs.getBigDecimal(column);
        return n == null ? null : n.toPlainString();
      }
      default -> {
        return integerOrText(rs, column, type);
      }
    }
  }

  private static Object integerOrText(ResultSet rs, String column, String type)
      throws SQLException {
    if (INTEGER_TYPES.contains(type)) {
      long v = rs.getLong(column);
      return rs.wasNull() ? null : Long.toString(v);
    }
    return rs.getString(column);
  }

  /**
   * The SQL placeholder of a value written to a column of a type: a plain placeholder for text and
   * booleans, a cast for the other types.
   *
   * @param type PostgreSQL type of the column
   * @return placeholder
   */
  public static String placeholder(String type) {
    if (TEXT_TYPES.contains(type) || "bool".equals(type)) {
      return "?";
    }
    return "cast(? as " + type + ")";
  }

  /**
   * The JDBC parameter of a canonical value written to a column of a type.
   *
   * @param value canonical value
   * @param type PostgreSQL type
   * @return parameter
   */
  public static Object parameter(Object value, String type) {
    if (value == null) {
      return null;
    }
    if ("bool".equals(type)) {
      return value instanceof Boolean b ? b : Boolean.valueOf(value.toString());
    }
    return value.toString();
  }
}
