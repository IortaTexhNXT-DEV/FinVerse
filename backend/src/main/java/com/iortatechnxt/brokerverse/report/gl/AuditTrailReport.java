package com.iortatechnxt.brokerverse.report.gl;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.service.AuditModules;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery.AuditEntry;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.NamedExport;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Audit trail of financial and non-financial activity (BDOI FRS FRUM.008.01 and FRPM.021.01):
 * timestamp, module, user ID (Windows ID), performed by, role at the time, action, activity, from
 * and to values, source address and reference, for the filters of the Audit Trail screen. Exported
 * as "Audit Logs_MMDDYYYY".
 */
@Component
public class AuditTrailReport implements ReportDefinition, NamedExport {

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
  private static final String ACTION = "action";
  private static final String ALL = "ALL";
  private static final String ENTITY_TYPE = "entityType";
  private static final String REFERENCE = "reference";

  private final AuditTrailQuery audit;

  /**
   * Creates the report.
   *
   * @param audit audit trail search
   */
  public AuditTrailReport(AuditTrailQuery audit) {
    this.audit = audit;
  }

  @Override
  public String exportName() {
    return "Audit Logs";
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
            ParameterSpec.optional(ENTITY_TYPE, "Entity Type", ParameterType.TEXT),
            ParameterSpec.optional(REFERENCE, "Reference Number", ParameterType.TEXT),
            ParameterSpec.select(ACTION, "Action Type", actions(), ALL)),
        Permission.AUDIT_VIEW);
  }

  private static List<String> actions() {
    List<String> values = new ArrayList<>();
    values.add(ALL);
    Arrays.stream(AuditAction.values()).map(Enum::name).forEach(values::add);
    return values;
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String action = p.optionalText(ACTION).orElse(ALL);
    AuditTrailQuery.Filter filter =
        new AuditTrailQuery.Filter(
            p.date(GlReportSupport.FROM),
            p.date(GlReportSupport.TO),
            p.optionalText("username").orElse(null),
            entityTypes(p.optionalText(ENTITY_TYPE).orElse(null)),
            p.optionalText(REFERENCE).orElse(null),
            ALL.equals(action) ? null : AuditAction.valueOf(action),
            "occurredAt",
            "asc");
    List<Map<String, Object>> rows = audit.all(filter).stream().map(AuditTrailReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("time", "Timestamp"),
            ReportColumn.text("module", "Module"),
            ReportColumn.text("userId", "User Id (Windows ID)"),
            ReportColumn.text("user", "Performed By"),
            ReportColumn.text("role", "Role"),
            ReportColumn.text(ACTION, "Action"),
            ReportColumn.text("summary", "Activity"),
            ReportColumn.text("from", "From (Old Value)"),
            ReportColumn.text("to", "To (New Value)"),
            ReportColumn.text("ip", "IP Address"),
            ReportColumn.text("key", "Reference"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note("Times in Philippine time.")
        .build();
  }

  private static List<String> entityTypes(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    if ("PRODUCT_MAINTENANCE".equals(value.trim())) {
      return AuditModules.PRODUCT_MAINTENANCE_TYPES;
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
  }

  private static Map<String, Object> row(AuditEntry e) {
    AuditLog a = e.log();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("time", TIME.format(a.getOccurredAt().atZone(BusinessClock.zone())));
    m.put("module", e.module());
    m.put("userId", e.windowsId());
    m.put("user", a.getUsername());
    m.put("role", a.getRoleNames());
    m.put(ACTION, e.actionLabel());
    m.put("summary", a.getSummary());
    m.put("from", a.getOldValue());
    m.put("to", a.getNewValue());
    m.put("ip", a.getIpAddress());
    m.put("key", a.getEntityId());
    return m;
  }
}
