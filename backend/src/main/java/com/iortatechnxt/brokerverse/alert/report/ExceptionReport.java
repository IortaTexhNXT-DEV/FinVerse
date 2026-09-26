package com.iortatechnxt.brokerverse.alert.report;

import com.iortatechnxt.brokerverse.alert.domain.Alert;
import com.iortatechnxt.brokerverse.alert.domain.AlertStatus;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.alert.service.AlertService.AlertSearch;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Exception report (CTL-EXCEPTIONS): alerts raised in a period, grouped by exception code, with
 * their handling status. Every module exception report is a filtered view of the same register.
 */
@Component
public class ExceptionReport implements ReportDefinition {

  private static final int MAX_ROWS = 20_000;
  private static final String FROM = "fromDate";
  private static final String TO = "toDate";
  private static final String STATUS = "status";
  private static final String ALL = "ALL";
  private static final String CODE = "code";

  private final AlertService alerts;

  /**
   * Creates the report.
   *
   * @param alerts alert service
   */
  public ExceptionReport(AlertService alerts) {
    this.alerts = alerts;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "CTL-EXCEPTIONS",
        "Exception Report",
        ReportCategory.CONTROL,
        "Exceptions raised by the control rules (Exception Codes Master) and how they were handled",
        List.of(
            ParameterSpec.optional("companyId", "Company", ParameterType.COMPANY),
            ParameterSpec.required(FROM, "From Date", ParameterType.DATE).withDefault("YEAR_START"),
            ParameterSpec.required(TO, "To Date", ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.select(
                STATUS, "Status", List.of(ALL, "OPEN", "ACKNOWLEDGED", "RESOLVED"), ALL),
            ParameterSpec.optional(CODE, "Exception Code", ParameterType.TEXT)),
        Permission.ALERT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String status = p.text(STATUS);
    AlertSearch criteria =
        new AlertSearch(
            ALL.equals(status) ? null : AlertStatus.valueOf(status),
            null,
            p.optionalText(CODE).orElse(null),
            p.optionalLong("companyId").orElse(null),
            p.date(FROM).atStartOfDay().toInstant(ZoneOffset.UTC),
            p.date(TO).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    List<Map<String, Object>> rows =
        alerts.search(criteria, PageRequest.of(0, MAX_ROWS)).getContent().stream()
            .map(ExceptionReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("raised", "Raised (UTC)"),
            ReportColumn.text("severity", "Severity"),
            ReportColumn.text("entity", "Record"),
            ReportColumn.text("message", "Exception"),
            ReportColumn.amountNoTotal("amount", "Amount"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("handledBy", "Handled By"),
            ReportColumn.text("comment", "Comment"))
        .groupBy(CODE, "Exception Code")
        .rows(rows)
        .withoutGrandTotal()
        .build();
  }

  private static Map<String, Object> row(Alert a) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(CODE, a.getExceptionCode());
    m.put("raised", a.getRaisedAt().toString());
    m.put("severity", a.getSeverity().name());
    m.put("entity", a.getEntityType() == null ? "" : a.getEntityType() + " " + a.getEntityId());
    m.put("message", a.getMessage());
    m.put("amount", a.getAmount());
    m.put(STATUS, a.getStatus().name());
    m.put("handledBy", a.getResolvedBy() != null ? a.getResolvedBy() : a.getAcknowledgedBy());
    m.put("comment", a.getStatusComment());
    return m;
  }
}
