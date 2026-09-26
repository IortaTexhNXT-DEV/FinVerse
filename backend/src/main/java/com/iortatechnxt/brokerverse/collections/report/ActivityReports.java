package com.iortatechnxt.brokerverse.collections.report;

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
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The Collections activity reports: reassignments (BRCLXN.052) and the audit log of field changes
 * with from / to values, user and source IP (BRCLXN.043/044), both for a period.
 */
public final class ActivityReports {

  /** Reassignments code. */
  public static final String REASSIGNMENTS = "CLX-REASSIGNMENTS";

  /** Audit log code. */
  public static final String AUDIT_LOG = "CLX-AUDIT-LOG";

  private static final String PERIOD = " between :from and :to";

  private ActivityReports() {}

  /** Reassignments of the period (BRCLXN.052): who moved which account to whom, and why. */
  @Component
  public static class Reassignments implements ReportDefinition {

    private static final String SQL =
        "select a.created_at, i.invoice_no, i.assured_name, i.segment, i.sales_unit,"
            + " a.previous_handler, a.handler_username, a.kind, a.valid_from, a.valid_to,"
            + " a.reason, a.assigned_by, a.bulk_ref, a.reverted_at"
            + " from clx_assignment a join clx_item i on i.id = a.item_id"
            + " where a.company_id = :companyId"
            + " and cast(a.created_at at time zone 'Asia/Manila' as date)"
            + PERIOD
            + ClxReportSql.ITEM_FILTERS
            + " order by a.id";

    private final ClxReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Reassignments(ClxReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ClxReportSql.metadata(
          REASSIGNMENTS,
          "Collection Reassignments",
          "Assignments and reassignments of collection accounts in the period (BRCLXN.052)",
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.date("created_at", "Date"),
              ReportColumn.text("invoice_no", "Invoice No."),
              ReportColumn.text("assured_name", "Name of Assured"),
              ReportColumn.text("segment", "Market Segment"),
              ReportColumn.text("sales_unit", "Sales Unit"),
              ReportColumn.text("previous_handler", "From Handler"),
              ReportColumn.text("handler_username", "To Handler"),
              ReportColumn.text("kind", "Kind"),
              ReportColumn.date("valid_from", "Valid From"),
              ReportColumn.date("valid_to", "Valid To"),
              ReportColumn.text("reason", "Reason"),
              ReportColumn.text("assigned_by", "Assigned By"),
              ReportColumn.text("bulk_ref", "Bulk Reference"),
              ReportColumn.date("reverted_at", "Ended On"))
          .rows(sql.rows(SQL, ClxReportSql.args(p)))
          .presorted()
          .withoutGrandTotal()
          .build();
    }
  }

  /**
   * Audit log (BRCLXN.043/044, NFR audit logging): every field change of the Collections records in
   * the period with the old and new values, user, time and source IP. Viewed and exported under
   * {@code CLX_AUDIT_VIEW}.
   */
  @Component
  public static class AuditLog implements ReportDefinition {

    private static final String SQL =
        "select c.changed_at, c.username, c.source_ip, c.entity, c.entity_id, c.field,"
            + " c.old_value, c.new_value, c.bulk_ref from clx_field_change c"
            + " where c.company_id = :companyId"
            + " and cast(c.changed_at at time zone 'Asia/Manila' as date)"
            + PERIOD
            + " and (cast(:user as varchar) is null or c.username = :user)"
            + " order by c.id";

    private final ClxReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public AuditLog(ClxReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return new ReportMetadata(
          AUDIT_LOG,
          "Collections Audit Log",
          ReportCategory.COLLECTIONS,
          "Field changes of the Collections records with from / to values (BRCLXN.043/044)",
          List.of(
              ParameterSpec.required(ClxReportSql.COMPANY, "Company", ParameterType.COMPANY),
              ParameterSpec.required(ClxReportSql.FROM, "From", ParameterType.DATE)
                  .withDefault("MONTH_START"),
              ParameterSpec.required(ClxReportSql.TO, "To", ParameterType.DATE)
                  .withDefault("TODAY"),
              ParameterSpec.optional("user", "User", ParameterType.TEXT)),
          Permission.CLX_AUDIT_VIEW,
          Permission.CLX_AUDIT_VIEW,
          true);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      var args = ClxReportSql.args(p);
      args.put("user", p.optionalText("user").filter(s -> !s.isBlank()).orElse(null));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.date("changed_at", "Changed On"),
              ReportColumn.text("username", "User ID"),
              ReportColumn.text("source_ip", "Source IP"),
              ReportColumn.text("entity", "Record"),
              ReportColumn.text("entity_id", "Reference"),
              ReportColumn.text("field", "Field"),
              ReportColumn.text("old_value", "From"),
              ReportColumn.text("new_value", "To"),
              ReportColumn.text("bulk_ref", "Bulk Reference"))
          .rows(sql.rows(SQL, args))
          .presorted()
          .withoutGrandTotal()
          .build();
    }
  }
}
