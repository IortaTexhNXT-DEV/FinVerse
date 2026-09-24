package com.iortatechnxt.brokerverse.nbreport.report;

import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared parameters and helpers of the New Business reports: every report is in the category "New
 * Business", runs for the company chosen in the header and, where it covers a period, takes a date
 * range (default: the current month).
 */
final class NbReportSupport {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** Period start parameter. */
  static final String FROM = "from";

  /** Period end parameter. */
  static final String TO = "to";

  /** "Any" option of the select filters. */
  static final String ALL = "ALL";

  private NbReportSupport() {}

  /**
   * Metadata of a New Business report with the company and, when {@code period}, the date range.
   *
   * @param code report code
   * @param title title
   * @param description one line purpose citing the BR ID
   * @param permission permission that may run it
   * @param period whether the report takes a date range
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code,
      String title,
      String description,
      Permission permission,
      boolean period,
      ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    if (period) {
      params.add(
          ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
      params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    }
    params.addAll(List.of(extra));
    return new ReportMetadata(
        code, title, ReportCategory.NEW_BUSINESS, description, params, permission);
  }

  /**
   * Bind parameters with the company and, when present, the date range.
   *
   * @param p report parameters
   * @return bind parameters
   */
  static SqlArgs args(ReportParameters p) {
    return SqlArgs.company(p.longValue(COMPANY))
        .with(FROM, p.optionalDate(FROM).orElse(null))
        .with(TO, p.optionalDate(TO).orElse(null));
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
    return p.optionalText(name).map(v -> v.strip().toUpperCase(Locale.ROOT)).orElse(null);
  }

  /**
   * "Yes" / "No" for a boolean cell.
   *
   * @param value boolean value (null reads as no)
   * @return text
   */
  static String yesNo(Object value) {
    return Boolean.TRUE.equals(value) ? "Yes" : "No";
  }

  /**
   * Replaces a boolean cell by "Yes" / "No".
   *
   * @param row row
   * @param key cell
   * @return the row
   */
  static Map<String, Object> flag(Map<String, Object> row, String key) {
    row.put(key, yesNo(row.get(key)));
    return row;
  }

  /**
   * A readable label for an upper-snake code ("POLICY_ISSUED" → "Policy issued").
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

  /**
   * Replaces a code cell by its readable label.
   *
   * @param row row
   * @param key cell
   * @return the row
   */
  static Map<String, Object> relabel(Map<String, Object> row, String key) {
    row.put(key, label(row.get(key)));
    return row;
  }
}
