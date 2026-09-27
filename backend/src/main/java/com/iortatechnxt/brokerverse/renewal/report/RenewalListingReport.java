package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renewal listing (RNW-LISTING; FR-RN-102, BRRN.036): the open renewals by unit, with the days to
 * expiry, the ageing window and the escalation flag of {@code RNW_ESCALATION_DAYS}.
 */
@Component
public class RenewalListingReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-LISTING";

  private static final int WINDOW = 30;
  private static final int WINDOWS = 3;

  private static final String SQL =
      "select c.owner_unit, c.renewal_ref, c.client_name, c.segment, c.product_code, c.stage,"
          + " c.assigned_ao, c.assigned_po, c.expiry_date"
          + " from rnw_candidate c where c.stage not in ('RENEWED', 'CLOSED')"
          + RenewalReportSupport.FILTERS
          + " order by c.owner_unit, c.expiry_date, c.renewal_ref";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;
  private final RenewalParameters parameters;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   * @param parameters escalation days
   * @param clock clock
   */
  public RenewalListingReport(
      NbReportJdbc jdbc, RenewalReportSupport support, RenewalParameters parameters, Clock clock) {
    this.jdbc = jdbc;
    this.support = support;
    this.parameters = parameters;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE, "Renewal Listing", "Open renewals by unit with the ageing to expiry and escalation");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate today = BusinessClock.today(clock);
    var rows =
        jdbc.rows(SQL, support.args(p).map()).stream()
            .map(RenewalReportSupport::labels)
            .map(r -> ageing(r, today))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("renewal_ref", "Renewal Reference"),
            ReportColumn.text("client_name", "Client"),
            ReportColumn.text("segment", "Segment"),
            ReportColumn.text("product_code", "Risk Code"),
            ReportColumn.text("stage", "Status"),
            ReportColumn.text("assigned_ao", "Account Officer"),
            ReportColumn.text("assigned_po", "Processing Officer"),
            ReportColumn.date("expiry_date", "Expiry Date"),
            ReportColumn.count("days", "Days to Expiry"),
            ReportColumn.text("window", "Window"),
            ReportColumn.text("escalated", "Escalated"))
        .groupBy("owner_unit", "Unit")
        .rows(rows)
        .presorted()
        .build();
  }

  private Map<String, Object> ageing(Map<String, Object> r, LocalDate today) {
    LocalDate expiry = (LocalDate) r.get("expiry_date");
    long days = ChronoUnit.DAYS.between(today, expiry);
    r.put("days", days);
    r.put("window", window(days));
    int limit = parameters.escalationDays((String) r.get("segment"));
    r.put("escalated", days <= limit ? "Yes" : "No");
    return r;
  }

  private static String window(long days) {
    if (days < 0) {
      return "Expired";
    }
    long bucket = days / WINDOW;
    return bucket >= WINDOWS
        ? "Over 90 days"
        : bucket * WINDOW + " to " + (bucket * WINDOW + WINDOW) + " days";
  }
}
