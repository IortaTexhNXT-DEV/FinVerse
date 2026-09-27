package com.iortatechnxt.brokerverse.report.core;

import java.util.List;
import java.util.Locale;

/**
 * The Origin filter of the operational reports that hold migrated records (DATA_MIGRATION_DESIGN
 * section 14): all records, the records created in BIBS, or the records migrated from the legacy
 * systems. The SQL filters its origin column with {@link #sql(String)}.
 */
public final class ReportOrigin {

  /** Parameter name. */
  public static final String PARAM = "origin";

  private static final String ALL = "ALL";

  private ReportOrigin() {}

  /**
   * The parameter: ALL (default), BIBS or MIGRATED.
   *
   * @return spec
   */
  public static ParameterSpec parameter() {
    return ParameterSpec.select(PARAM, "Origin", List.of(ALL, "BIBS", "MIGRATED"), ALL);
  }

  /**
   * The bind value, null for all records.
   *
   * @param p parameters
   * @return BIBS, MIGRATED or null
   */
  public static String value(ReportParameters p) {
    return p.optionalText(PARAM)
        .map(v -> v.strip().toUpperCase(Locale.ROOT))
        .filter(v -> !v.isEmpty() && !ALL.equals(v))
        .orElse(null);
  }

  /**
   * The SQL condition on an origin column (named parameter {@code :origin}).
   *
   * @param column origin column, e.g. {@code i.origin}
   * @return {@code and ...} condition
   */
  public static String sql(String column) {
    return " and (cast(:origin as varchar) is null or " + column + " = :origin)";
  }
}
