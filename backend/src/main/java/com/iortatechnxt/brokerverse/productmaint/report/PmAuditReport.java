package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery.AuditEntry;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.service.PmAuditScope;
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
 * The Product Maintenance audit logs as a file (BDOI FRS FRPM.021.01 and FRPM.021.02, PM-AUDIT):
 * Timestamp, Module, Reference Number, Client/Assured's Name, Action Type, Description, Old Value,
 * New Value, Performed By and Remarks, for the filters on screen or for one record; named "Audit
 * Logs_MMDDYYYY".
 */
@Component
public class PmAuditReport implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "PM-AUDIT";

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
  private static final String FROM = "fromDate";
  private static final String TO = "toDate";
  private static final String USER = "username";
  private static final String TYPE = "entityType";
  private static final String REFERENCE = "reference";
  private static final String ACTION = "action";
  private static final String ALL = "ALL";

  private final AuditTrailQuery audit;
  private final UserDisplayNames names;

  /**
   * Creates the report.
   *
   * @param audit audit trail search
   * @param names user names
   */
  public PmAuditReport(AuditTrailQuery audit, UserDisplayNames names) {
    this.audit = audit;
    this.names = names;
  }

  @Override
  public String exportName() {
    return "Audit Logs";
  }

  @Override
  public ReportMetadata metadata() {
    List<String> actions = new ArrayList<>(List.of(ALL));
    Arrays.stream(AuditAction.values()).map(Enum::name).forEach(actions::add);
    return new ReportMetadata(
        CODE,
        "Product Maintenance Audit Logs",
        ReportCategory.NEW_BUSINESS,
        "Who changed the packages, products and requests of Product Maintenance, and when",
        List.of(
            ParameterSpec.required(FROM, "From", ParameterType.DATE),
            ParameterSpec.required(TO, "To", ParameterType.DATE),
            ParameterSpec.optional(USER, "User", ParameterType.TEXT),
            ParameterSpec.optional(TYPE, "Record Type", ParameterType.TEXT),
            ParameterSpec.optional(REFERENCE, "Reference Number", ParameterType.TEXT),
            ParameterSpec.select(ACTION, "Action Type", actions, ALL)),
        Permission.PKG_REPORT_VIEW,
        null,
        false);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String action = p.optionalText(ACTION).orElse(ALL);
    String reference = p.optionalText(REFERENCE).orElse(null);
    AuditTrailQuery.Filter filter =
        new AuditTrailQuery.Filter(
            p.date(FROM),
            p.date(TO),
            p.optionalText(USER).orElse(null),
            PmAuditScope.types(p.optionalText(TYPE).orElse(null), reference),
            reference,
            ALL.equals(action) ? null : AuditAction.valueOf(action),
            "occurredAt",
            "asc");
    List<Map<String, Object>> rows = audit.all(filter).stream().map(this::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("time", "Timestamp"),
            ReportColumn.text("module", "Module"),
            ReportColumn.text(REFERENCE, "Reference Number"),
            ReportColumn.text("subject", "Client/Assured's Name"),
            ReportColumn.text(ACTION, "Action Type"),
            ReportColumn.text("summary", "Description"),
            ReportColumn.text("from", "Old Value"),
            ReportColumn.text("to", "New Value"),
            ReportColumn.text("user", "Performed By"),
            ReportColumn.text("remarks", "Remarks"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note("Times in Philippine time.")
        .build();
  }

  private Map<String, Object> row(AuditEntry e) {
    AuditLog a = e.log();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("time", TIME.format(a.getOccurredAt().atZone(BusinessClock.zone())));
    m.put("module", e.module());
    m.put(REFERENCE, a.getEntityId());
    m.put("subject", e.subject());
    m.put(ACTION, e.actionLabel());
    m.put("summary", a.getSummary());
    m.put("from", a.getOldValue());
    m.put("to", a.getNewValue());
    m.put("user", names.displayName(a.getUsername()));
    m.put("remarks", a.getRemarks());
    return m;
  }
}
