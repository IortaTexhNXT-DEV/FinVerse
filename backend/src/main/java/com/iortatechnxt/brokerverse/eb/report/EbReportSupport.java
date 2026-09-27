package com.iortatechnxt.brokerverse.eb.report;

import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared parameters of the Employee Benefits reports (FR-EB-060 to 062; category "Employee
 * Benefits", view and export {@code EB_REPORT_VIEW}): company, date range and the common filters
 * team, account officer, client, benefit line, insurer and business type.
 */
final class EbReportSupport {

  static final String COMPANY = "companyId";
  static final String FROM = "from";
  static final String TO = "to";
  static final String TEAM = "team";
  static final String AO = "ao";
  static final String CLIENT = "client";
  static final String LINE = "benefitLine";
  static final String INSURER = "insurer";
  static final String BUSINESS_TYPE = "businessType";
  static final String ALL = "ALL";

  /** Common filters on the programme {@code p}. */
  static final String PROGRAMME_FILTERS =
      " and (cast(:team as varchar) is null or p.team_code = :team)"
          + " and (cast(:ao as varchar) is null or lower(p.account_officer) = lower(:ao))"
          + " and (cast(:client as varchar) is null or p.client_code = :client"
          + " or lower(p.client_name) like '%' || lower(cast(:client as varchar)) || '%')";

  private EbReportSupport() {}

  /**
   * Metadata with the company, the period and the common filters.
   *
   * @param code report code
   * @param title title
   * @param description purpose
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
    params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    params.add(
        ParameterSpec.select(
            TEAM,
            "Team",
            List.of(ALL, "BDO", "SM", "VOLUNTARY", "SOLICITED", "NEW_BUSINESS"),
            ALL));
    params.add(ParameterSpec.optional(AO, "Account Officer", ParameterType.TEXT));
    params.add(ParameterSpec.optional(CLIENT, "Client", ParameterType.TEXT));
    params.add(ParameterSpec.select(LINE, "Benefit Line", List.of(ALL, "HMO", "GLI", "GPA"), ALL));
    params.add(ParameterSpec.optional(INSURER, "Insurer", ParameterType.TEXT));
    params.add(
        ParameterSpec.select(
            BUSINESS_TYPE, "Business Type", List.of(ALL, "NEW_BUSINESS", "RENEWAL"), ALL));
    params.addAll(List.of(extra));
    return ReportMetadata.employeeBenefits(code, title, description, params);
  }

  /**
   * Bind parameters of the common filters.
   *
   * @param p report parameters
   * @return bind parameters
   */
  static SqlArgs args(ReportParameters p) {
    return SqlArgs.company(p.longValue(COMPANY))
        .with(FROM, p.optionalDate(FROM).orElse(null))
        .with(TO, p.optionalDate(TO).orElse(null))
        .with(TEAM, selected(p, TEAM))
        .with(AO, text(p, AO))
        .with(CLIENT, text(p, CLIENT))
        .with(LINE, selected(p, LINE))
        .with(INSURER, upper(p, INSURER))
        .with(BUSINESS_TYPE, selected(p, BUSINESS_TYPE));
  }

  /**
   * A select filter, null for all.
   *
   * @param p parameters
   * @param name parameter
   * @return value or null
   */
  static String selected(ReportParameters p, String name) {
    return p.optionalText(name).filter(v -> !ALL.equals(v) && !v.isBlank()).orElse(null);
  }

  private static String text(ReportParameters p, String name) {
    return p.optionalText(name).map(String::strip).filter(v -> !v.isEmpty()).orElse(null);
  }

  private static String upper(ReportParameters p, String name) {
    String value = text(p, name);
    return value == null ? null : value.toUpperCase(Locale.ROOT);
  }

  /**
   * A readable label of a code ("WITH_CLIENT" to "With client").
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
   * Replaces code cells by their readable labels.
   *
   * @param row row
   * @param keys cells
   * @return the row
   */
  static Map<String, Object> relabel(Map<String, Object> row, String... keys) {
    for (String key : keys) {
      row.put(key, label(row.get(key)));
    }
    return row;
  }
}
