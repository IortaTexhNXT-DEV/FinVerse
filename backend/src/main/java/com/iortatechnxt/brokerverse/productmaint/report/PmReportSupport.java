package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared parameters and helpers of the Product Maintenance reports (BRPM.017/018): category "New
 * Business", the company of the header, run and export with PKG_REPORT_VIEW; SQL values are
 * normalised for the renderers (dates and timestamps become Philippine dates).
 */
final class PmReportSupport {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** "Any" option of the select filters. */
  static final String ALL = "ALL";

  /** Business time zone of BDOI. */
  static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private PmReportSupport() {}

  /**
   * Metadata of a Product Maintenance report.
   *
   * @param code report code
   * @param title title
   * @param description one line purpose citing the BR ID
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    params.addAll(List.of(extra));
    return new ReportMetadata(
        code,
        title,
        ReportCategory.NEW_BUSINESS,
        description,
        params,
        Permission.PKG_REPORT_VIEW,
        null,
        false);
  }

  /**
   * A select filter's value, null for "all".
   *
   * @param p parameters
   * @param name parameter
   * @return value or null
   */
  static String selected(ReportParameters p, String name) {
    return p.optionalText(name).filter(v -> !ALL.equals(v)).orElse(null);
  }

  /**
   * An optional text filter, upper-cased and trimmed, null when blank.
   *
   * @param p parameters
   * @param name parameter
   * @return value or null
   */
  static String upper(ReportParameters p, String name) {
    return p.optionalText(name)
        .map(v -> v.strip().toUpperCase(Locale.ROOT))
        .filter(v -> !v.isEmpty())
        .orElse(null);
  }

  /**
   * A row with SQL dates and timestamps as local dates.
   *
   * @param row JDBC row
   * @return normalised row
   */
  static Map<String, Object> normalise(Map<String, Object> row) {
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

  /**
   * A readable label for an upper-snake code ("FOR_MANCOM" → "For mancom").
   *
   * @param code code
   * @return label, empty for null
   */
  static String label(Object code) {
    if (code == null) {
      return "";
    }
    String text = code.toString().replace('_', ' ').toLowerCase(Locale.ROOT);
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
