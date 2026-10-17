package com.iortatechnxt.brokerverse.prodrecon.report;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@code PRC-LEGACY-CHANGES} (DATA_MIGRATION_DESIGN 14.4 I; BRID 10.1): the production of the
 * legacy invoices was reconciled in the legacy systems, so the reconciliation shows instead every
 * change made in BIBS to a migrated invoice or its endorsements in the period - one line per change
 * and component with the original amount at cut-over (origin snapshot), the current amount and the
 * delta, the change reference, date and user. Payments and remittances are listed only on request.
 */
@Component
public class LegacyChangesReport implements ReportDefinition {

  static final String SOURCE = "sourceSystem";
  static final String CHANGE = "changeType";
  static final String PAYMENTS = "payments";

  private static final String ALL = "ALL";
  private static final String NO = "NO";
  private static final String INSURER = "insurer";

  private static final String SQL =
      "select i.insurer_code as insurer, coalesce(r.legacy_invoice_no, i.legacy_invoice_no)"
          + " as legacy_invoice_no, i.invoice_no, coalesce(r.source_system, i.source_system)"
          + " as source_system, m.value_date as change_date, "
          + "case when m.source_ref like 'MIG:TU:%' then 'FY2027 TRUE-UP'"
          + " when m.movement_type = 'BOOKED' then 'ENDORSEMENT'"
          + " else m.movement_type end as change_type, m.component,"
          + " ol.booked + ol.adjusted as original, c.booked + c.adjusted as updated,"
          + " m.amount as delta, ol.open_balance as original_balance, c.balance as balance,"
          + " m.source_ref as change_ref, m.posted_by as changed_by"
          + " from ops_invoice_movement m join ops_invoice i on i.id = m.invoice_id"
          + " left join ops_invoice r on r.invoice_no = i.parent_invoice_no"
          + " left join ops_invoice_component c on c.invoice_id = i.id and c.component = m.component"
          + " left join ops_invoice_origin_snapshot s"
          + " on s.invoice_id = coalesce(r.id, i.id)"
          + " left join ops_invoice_origin_line ol on ol.snapshot_id = s.id"
          + " and ol.component = m.component"
          + " where i.company_id = :companyId and i.ledger_context = 'LEGACY'"
          + " and m.source_ref not like 'MIG:INV:%'"
          + " and m.value_date between :from and :to"
          + " and (cast(:insurer as varchar) is null or i.insurer_code = :insurer)"
          + " and (cast(:sourceSystem as varchar) is null"
          + " or coalesce(r.source_system, i.source_system) = :sourceSystem)"
          + " and (:payments = 'YES' or m.movement_type not in ('APPLIED', 'UNAPPLIED', 'REMITTED'))"
          + " and (cast(:changeType as varchar) is null or case when m.source_ref like 'MIG:TU:%'"
          + " then 'FY2027 TRUE-UP' when m.movement_type = 'BOOKED' then 'ENDORSEMENT'"
          + " else m.movement_type end = :changeType)"
          + " order by i.insurer_code, 2, m.value_date, m.id";

  private final ReconReportSupport support;

  /**
   * Creates the report.
   *
   * @param support report SQL
   */
  public LegacyChangesReport(ReconReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params =
        new ArrayList<>(
            ReconReportSupport.metadata(
                    "PRC-LEGACY-CHANGES",
                    "Changes to Legacy Invoices",
                    "Original, updated and delta of every change made in BIBS to a migrated invoice")
                .parameters());
    params.add(ParameterSpec.optional(SOURCE, "Legacy System", ParameterType.TEXT));
    params.add(
        ParameterSpec.select(
            CHANGE,
            "Change Type",
            List.of(
                ALL,
                "ENDORSEMENT",
                "ADJUSTED",
                "CORRECTION",
                "DP_REVERSAL",
                "CWT_RECLASS",
                "WRITE_OFF",
                "MIN_BAL",
                "FY2027 TRUE-UP"),
            ALL));
    params.add(
        ParameterSpec.select(PAYMENTS, "Include Payments and Remittances", List.of(NO, "YES"), NO));
    return new ReportMetadata(
        "PRC-LEGACY-CHANGES",
        "Changes to Legacy Invoices",
        ReportCategory.OPERATIONS,
        "Original, updated and delta of every change made in BIBS to a migrated invoice",
        params,
        Permission.RECON_PROCESS,
        Permission.RECON_PROCESS,
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = ReconReportSupport.args(p);
    args.put(
        SOURCE,
        p.optionalText(SOURCE)
            .filter(v -> !v.isBlank())
            .map(v -> v.strip().toUpperCase(Locale.ROOT))
            .orElse(null));
    args.put(CHANGE, p.optionalText(CHANGE).filter(v -> !ALL.equals(v)).orElse(null));
    args.put(PAYMENTS, p.optionalText(PAYMENTS).orElse(NO));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("legacy_invoice_no", "Legacy Invoice"),
            ReportColumn.text("invoice_no", "BIBS Invoice"),
            ReportColumn.text("source_system", "System"),
            ReportColumn.date("change_date", "Date"),
            ReportColumn.text("change_type", "Change"),
            ReportColumn.text("component", "Component"),
            ReportColumn.amountNoTotal("original", "Original"),
            ReportColumn.amountNoTotal("updated", "Updated"),
            ReportColumn.amount("delta", "Delta"),
            ReportColumn.amountNoTotal("original_balance", "Balance at Cut-over"),
            ReportColumn.amountNoTotal("balance", "Balance Now"),
            ReportColumn.text("change_ref", "Reference"),
            ReportColumn.text("changed_by", "User"))
        .groupBy(INSURER, "Insurance Company")
        .rows(support.rows(SQL, args))
        .presorted()
        .build();
  }
}
