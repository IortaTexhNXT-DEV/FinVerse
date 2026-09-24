package com.iortatechnxt.brokerverse.report.gl;

import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.domain.AuditLogRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Audit trail of financial and non-financial activity by user and entity. */
@Component
public class AuditTrailReport implements ReportDefinition {

  private static final int MAX_ROWS = 20_000;

  private final AuditLogRepository audit;

  /**
   * Creates the report.
   *
   * @param audit audit repository
   */
  public AuditTrailReport(AuditLogRepository audit) {
    this.audit = audit;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "CTL-AUDIT",
        "Audit Trail Report",
        ReportCategory.CONTROL,
        "Who did what and when: originator, modifier and authorizer activity",
        List.of(
            GlReportSupport.fromParam(),
            GlReportSupport.toParam(),
            ParameterSpec.optional("username", "User", ParameterType.TEXT),
            ParameterSpec.optional("entityType", "Entity Type", ParameterType.TEXT)),
        Permission.AUDIT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var page =
        audit.search(
            p.optionalText("username").orElse(null),
            p.optionalText("entityType").orElse(null),
            null,
            p.date(GlReportSupport.FROM).atStartOfDay().toInstant(ZoneOffset.UTC),
            p.date(GlReportSupport.TO).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
            PageRequest.of(0, MAX_ROWS, Sort.by("occurredAt")));
    List<Map<String, Object>> rows = page.getContent().stream().map(AuditTrailReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("time", "Timestamp (UTC)"),
            ReportColumn.text("user", "User"),
            ReportColumn.text("action", "Action"),
            ReportColumn.text("entity", "Entity"),
            ReportColumn.text("key", "Reference"),
            ReportColumn.text("summary", "Details"))
        .rows(rows)
        .withoutGrandTotal()
        .build();
  }

  private static Map<String, Object> row(AuditLog a) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("time", a.getOccurredAt().toString());
    m.put("user", a.getUsername());
    m.put("action", a.getAction().name());
    m.put("entity", a.getEntityType());
    m.put("key", a.getEntityId());
    m.put("summary", a.getSummary());
    return m;
  }
}
