package com.iortatechnxt.brokerverse.csf.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Shared parameters, SQL and labels of the CSF reports (CUSTOMER_SERVICING_DESIGN section 9;
 * FR-CSF-041, 042): company, a date range on the business days of Manila, an optional agent and
 * client; list codes are shown by their labels and users by their names.
 */
@Component
public class CsfReportSupport {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** First day parameter (range convention fromDate / toDate). */
  static final String FROM = "fromDate";

  /** Last day parameter. */
  static final String TO = "toDate";

  /** Agent parameter (user ID). */
  static final String AGENT = "agent";

  /** Client parameter (client code). */
  static final String CLIENT = "client";

  /** Time of day in lists (dd-MMM-yyyy HH:mm, Philippine time). */
  static final String TIME_FORMAT = "'DD-Mon-YYYY HH24:MI'";

  private final NbReportJdbc jdbc;
  private final LovService lovs;

  /**
   * Creates the support.
   *
   * @param jdbc report SQL
   * @param lovs list labels
   */
  public CsfReportSupport(NbReportJdbc jdbc, LovService lovs) {
    this.jdbc = jdbc;
    this.lovs = lovs;
  }

  /**
   * The common parameters followed by the report's own.
   *
   * @param extra report parameters
   * @return parameters
   */
  static List<ParameterSpec> parameters(ParameterSpec... extra) {
    List<ParameterSpec> specs = new ArrayList<>();
    specs.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    specs.add(ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
    specs.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    specs.add(ParameterSpec.optional(AGENT, "Agent (user ID)", ParameterType.TEXT));
    specs.addAll(List.of(extra));
    return specs;
  }

  /**
   * The business day of a timestamp column in SQL.
   *
   * @param column timestamp column
   * @return expression
   */
  static String day(String column) {
    return "cast(" + column + " at time zone '" + BusinessClock.zoneId() + "' as date)";
  }

  /**
   * A timestamp column as dd-MMM-yyyy HH:mm in Philippine time.
   *
   * @param column timestamp column
   * @return expression
   */
  static String time(String column) {
    return "to_char("
        + column
        + " at time zone '"
        + BusinessClock.zoneId()
        + "', "
        + TIME_FORMAT
        + ")";
  }

  /**
   * Runs a report SQL with the company, the date range, the agent and the client.
   *
   * @param sql SQL with {@code :company}, {@code :from}, {@code :to}, {@code :agent}, {@code
   *     :client} and further named parameters
   * @param p parameters
   * @param extra further named parameters
   * @return rows
   */
  List<Map<String, Object>> rows(String sql, ReportParameters p, Map<String, Object> extra) {
    SqlArgs args =
        SqlArgs.company(p.longValue(COMPANY))
            .with("from", p.date(FROM))
            .with("to", p.date(TO))
            .with(
                AGENT,
                p.optionalText(AGENT).map(a -> a.strip().toLowerCase(Locale.ROOT)).orElse(""))
            .with(
                CLIENT,
                p.optionalText(CLIENT).map(c -> c.strip().toUpperCase(Locale.ROOT)).orElse(""));
    extra.forEach(args::with);
    return jdbc.rows(sql, args.map());
  }

  /**
   * Replaces a list code by its label.
   *
   * @param row row
   * @param key column
   * @param lov list
   */
  void label(Map<String, Object> row, String key, String lov) {
    Object v = row.get(key);
    if (v != null) {
      row.put(key, lovs.label(lov, v.toString()));
    }
  }

  /**
   * The label of a list code.
   *
   * @param lov list
   * @param code code
   * @return label
   */
  String labelOf(String lov, String code) {
    return lovs.label(lov, code);
  }

  /**
   * A code in words ("NOT_CONFIGURED" reads "Not configured").
   *
   * @param code code
   * @return words, null for null
   */
  static String words(Object code) {
    if (code == null) {
      return null;
    }
    String text = code.toString().replace('_', ' ').toLowerCase(Locale.ROOT);
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
