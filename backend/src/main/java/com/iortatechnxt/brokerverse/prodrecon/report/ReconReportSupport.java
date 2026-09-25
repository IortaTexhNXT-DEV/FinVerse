package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared parameters and SQL access of the production reconciliation reports (Annex IV, PRCID
 * .017-019/028/034-039): Operations category (view / export permissions, archived runs), the
 * company, the production months from / to (any day of the month) and an optional insurer. Report
 * layouts BDOI has not given are drafts to confirm (OQ42).
 */
@Component
@Transactional(readOnly = true)
public class ReconReportSupport {

  /** Company parameter. */
  static final String COMPANY = "companyId";

  /** First production month. */
  static final String FROM = "from";

  /** Last production month. */
  static final String TO = "to";

  /** Insurer filter. */
  static final String INSURER = "insurer";

  /** Amount of an item: the booked gross premium, else the insurer's. */
  static final String AMOUNT = "coalesce(i.bdoi_gross_premium, i.ins_gross_premium, 0)";

  /** Items of the company's cycles in the period, optionally of one insurer. */
  static final String ITEMS_OF_PERIOD =
      " from prc_item i join prc_cycle c on c.id = i.cycle_id"
          + " where c.company_id = :companyId and c.production_month between :from and :to"
          + " and (cast(:insurer as varchar) is null or c.insurer_code = :insurer)";

  private final NbReportJdbc jdbc;

  /**
   * Creates the support.
   *
   * @param jdbc read-only report SQL (dates normalised for the renderers)
   */
  public ReconReportSupport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Metadata with the company, the production months and the insurer.
   *
   * @param code report code
   * @param title title
   * @param description purpose with the BR ID
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    params.add(
        ParameterSpec.required(FROM, "From Month", ParameterType.DATE).withDefault("YEAR_START"));
    params.add(ParameterSpec.required(TO, "To Month", ParameterType.DATE).withDefault("TODAY"));
    params.add(ParameterSpec.optional(INSURER, "Insurer Code", ParameterType.TEXT));
    params.addAll(List.of(extra));
    return ReportMetadata.operations(code, title, description, params);
  }

  /**
   * Bind parameters: company, the months widened to whole months, the insurer.
   *
   * @param p report parameters
   * @return bind parameters
   */
  static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.longValue(COMPANY));
    args.put(FROM, p.date(FROM).withDayOfMonth(1));
    LocalDate to = p.date(TO);
    args.put(TO, to.withDayOfMonth(to.lengthOfMonth()));
    args.put(
        INSURER,
        p.optionalText(INSURER)
            .filter(v -> !v.isBlank())
            .map(v -> v.strip().toUpperCase(Locale.ROOT))
            .orElse(null));
    return args;
  }

  /**
   * Runs a report query.
   *
   * @param sql constant SQL
   * @param args bind parameters
   * @return rows
   */
  public List<Map<String, Object>> rows(String sql, Map<String, Object> args) {
    return jdbc.rows(sql, args);
  }
}
