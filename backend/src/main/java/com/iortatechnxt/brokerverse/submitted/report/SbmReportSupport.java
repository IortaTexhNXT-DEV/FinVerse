package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Shared parameters, scope and labels of the Submitted Policies reports (SUBMITTED_POLICIES_DESIGN
 * section 11): company and an optional date range; the masterlist {@code p} is restricted to the
 * user's scope where the SQL carries {@value #SCOPE}; segment, bucket and status codes are shown by
 * their names.
 */
@Component
public class SbmReportSupport {

  /** Placeholder of the scope condition in a report SQL. */
  public static final String SCOPE = "{scope}";

  /** Filter of the date range on a column, with the {@code :from} and {@code :to} parameters. */
  public static final String RANGE =
      " and (cast(:from as date) is null or cast(%1$s as date) >= :from)"
          + " and (cast(:to as date) is null or cast(%1$s as date) <= :to)";

  private static final String COMPANY = "companyId";
  private static final String FROM = "from";
  private static final String TO = "to";

  private final NbReportJdbc jdbc;
  private final SbmScopeService scopes;
  private final LovService lovs;

  /**
   * Creates the support.
   *
   * @param jdbc report SQL
   * @param scopes user scope
   * @param lovs list labels
   */
  public SbmReportSupport(NbReportJdbc jdbc, SbmScopeService scopes, LovService lovs) {
    this.jdbc = jdbc;
    this.scopes = scopes;
    this.lovs = lovs;
  }

  /**
   * Metadata with the company and the date range.
   *
   * @param code code
   * @param title title
   * @param description purpose
   * @param dateLabel what the date range filters
   * @return metadata
   */
  static ReportMetadata metadata(String code, String title, String description, String dateLabel) {
    return ReportMetadata.submitted(
        code,
        title,
        description,
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.optional(FROM, dateLabel + " From", ParameterType.DATE),
            ParameterSpec.optional(TO, dateLabel + " To", ParameterType.DATE)));
  }

  /**
   * Runs a report SQL with the company, the date range and the user's scope.
   *
   * @param sql SQL with {@code :company}, {@code :from}, {@code :to} and {@value #SCOPE}
   * @param p parameters
   * @return rows with names in place of codes
   */
  List<Map<String, Object>> rows(String sql, ReportParameters p) {
    long companyId = p.longValue(COMPANY);
    SbmScopeService.Scope scope = scopes.current(companyId);
    SqlArgs args =
        SqlArgs.company(companyId)
            .with(FROM, p.optionalDate(FROM).orElse(null))
            .with(TO, p.optionalDate(TO).orElse(null))
            .with("scopeSegments", scope.segments().isEmpty() ? List.of("") : scope.segments())
            .with("scopeUser", scope.username() == null ? "" : scope.username());
    return jdbc.rows(sql.replace(SCOPE, scope.sql()), args.map()).stream()
        .map(this::labels)
        .toList();
  }

  private Map<String, Object> labels(Map<String, Object> row) {
    label(row, "segment", SubmittedCodes.LOV_SEGMENT);
    label(row, "bucket", SubmittedCodes.LOV_BUCKET);
    label(row, "conversion_status", SubmittedCodes.LOV_CONVERSION);
    for (String key : List.of("status", "classification", "outcome", "step", "handoff_status")) {
      Object v = row.get(key);
      if (v != null) {
        row.put(key, words(v.toString()));
      }
    }
    return row;
  }

  private void label(Map<String, Object> row, String key, String lov) {
    Object v = row.get(key);
    if (v != null) {
      row.put(key, lovs.label(lov, v.toString()));
    }
  }

  /**
   * A code in words ("FOR_RENEWAL" reads "For renewal").
   *
   * @param code code
   * @return words
   */
  static String words(String code) {
    String text = code.replace('_', ' ').toLowerCase(Locale.ROOT);
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
