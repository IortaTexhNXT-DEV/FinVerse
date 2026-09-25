package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Package Status Update Report (PM-PKG-STATUS, BRPM.018; the name is PQ15): where each package
 * request stands, for one request or all, with its type, client or programme, product and resulting
 * version, stage and stage age, package end date, scheme rate, chosen insurers and the last action.
 * Filters: status, type, line, insurer and package end date within n days.
 */
@Component
public class PackageStatusReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PM-PKG-STATUS";

  private static final String REQUEST = "request";
  private static final String STATUS = "status";
  private static final String TYPE = "type";
  private static final String LINE = "line";
  private static final String INSURER = "insurer";
  private static final String WITHIN = "within";
  private static final String STAGE = "stage";

  private static final String SQL =
      "select r.request_no as request, r.request_type as type, r.scope,"
          + " coalesce(r.client_name, 'Programme') as client, r.title, r.line_code as line,"
          + " r.target_product_code as product, r.resulting_version_no as version,"
          + " r.status as stage, (cast(:today as date)"
          + " - cast(c.stage_entered_at at time zone 'Asia/Manila' as date)) as stage_age,"
          + " r.package_end_date as end_date, r.scheme_rate as rate, r.chosen_insurers as insurers,"
          + " h.action as last_action, h.occurred_at as last_action_at, h.actor as last_actor"
          + " from pm_request r"
          + " join wf_case c on c.entity_type = 'PackageRequest'"
          + " and c.entity_id = cast(r.id as varchar)"
          + " left join lateral (select x.action, x.occurred_at, x.actor from wf_case_history x"
          + " where x.case_id = c.id order by x.id desc limit 1) h on true"
          + " where r.company_id = :company"
          + " and (cast(:request as varchar) is null or r.request_no = :request)"
          + " and (cast(:status as varchar) is null or r.status = :status)"
          + " and (cast(:type as varchar) is null or r.request_type = :type)"
          + " and (cast(:line as varchar) is null or r.line_code = :line)"
          + " and (cast(:insurer as varchar) is null or r.chosen_insurers like '%' || :insurer || '%')"
          + " and (cast(:until as date) is null or r.package_end_date <= cast(:until as date))"
          + " order by r.request_no";

  private final NamedParameterJdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC template
   * @param clock clock
   */
  public PackageStatusReport(NamedParameterJdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    List<String> stages = new ArrayList<>(List.of(PmReportSupport.ALL));
    Arrays.stream(RequestStage.values()).map(Enum::name).forEach(stages::add);
    List<String> types = new ArrayList<>(List.of(PmReportSupport.ALL));
    Arrays.stream(RequestType.values()).map(Enum::name).forEach(types::add);
    return PmReportSupport.metadata(
        CODE,
        "Package Status Update Report",
        "Status of package requests, one or all, with stage age, end date, rates and the last"
            + " action (BRPM.018)",
        ParameterSpec.optional(REQUEST, "Request No. (individual report)", ParameterType.TEXT),
        ParameterSpec.select(STATUS, "Status", stages, PmReportSupport.ALL),
        ParameterSpec.select(TYPE, "Request Type", types, PmReportSupport.ALL),
        ParameterSpec.optional(LINE, "Line Code", ParameterType.TEXT),
        ParameterSpec.optional(INSURER, "Insurer Code", ParameterType.TEXT),
        ParameterSpec.optional(WITHIN, "Package Ends Within (days)", ParameterType.NUMBER));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate today = LocalDate.now(clock);
    MapSqlParameterSource args =
        new MapSqlParameterSource()
            .addValue("company", p.longValue(PmReportSupport.COMPANY))
            .addValue("today", today)
            .addValue(REQUEST, PmReportSupport.upper(p, REQUEST))
            .addValue(STATUS, PmReportSupport.selected(p, STATUS))
            .addValue(TYPE, PmReportSupport.selected(p, TYPE))
            .addValue(LINE, PmReportSupport.upper(p, LINE))
            .addValue(INSURER, PmReportSupport.upper(p, INSURER))
            .addValue("until", p.optionalLong(WITHIN).map(today::plusDays).orElse(null));
    List<Map<String, Object>> rows =
        jdbc.queryForList(SQL, args).stream()
            .map(PmReportSupport::normalise)
            .map(
                r -> {
                  r.put(STAGE, PmReportSupport.label(r.get(STAGE)));
                  r.put("last_action", PmReportSupport.label(r.get("last_action")));
                  return r;
                })
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(REQUEST, "Request No."),
            ReportColumn.text(TYPE, "Type"),
            ReportColumn.text("client", "Client / Programme"),
            ReportColumn.text("title", "Package"),
            ReportColumn.text("product", "Product"),
            ReportColumn.count("version", "Version"),
            ReportColumn.text(STAGE, "Stage"),
            ReportColumn.count("stage_age", "Days in Stage"),
            ReportColumn.date("end_date", "Package End"),
            ReportColumn.percent("rate", "Rate %"),
            ReportColumn.text("insurers", "Insurers"),
            ReportColumn.text("last_action", "Last Action"),
            ReportColumn.date("last_action_at", "On"),
            ReportColumn.text("last_actor", "By"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }
}
