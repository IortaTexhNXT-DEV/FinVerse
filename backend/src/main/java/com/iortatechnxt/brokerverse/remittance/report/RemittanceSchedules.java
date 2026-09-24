package com.iortatechnxt.brokerverse.remittance.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The remittance schedules of Annex III (RMTID.011/039): Normal (#3), Special (#4) and With
 * Incentives (#7), one line per account of the batches extracted in the period (or of one batch),
 * grouped by batch. The Mall Assurance columns of the Normal schedule and the incentive expiry date
 * wait for their definitions (OQ42, OQ23).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the reports
public final class RemittanceSchedules {

  private static final String BATCH = "batchNo";
  private static final String SQL =
      "select b.batch_no as batch, l.invoice_no, l.policy_no, l.endorsement_no, l.assured_name,"
          + " l.risk_code, case when l.excluded then 'EXCLUDED' else b.stage end as record_status,"
          + " l.last_paid_on, l.inception_date, l.booking_date, l.expiry_date, l.paid_ar,"
          + " l.commission, l.commission_vat, l.wtax, l.dtip, l.net_due, l.basic_premium,"
          + " l.incentive + l.incentive_vat as incentive,"
          + " l.net_due - l.incentive - l.incentive_vat as net_after_incentive,"
          + " l.insurer_or_date, l.insurer_or_no, l.insurer_or_amount"
          + " from rem_batch_line l join rem_batch b on b.id = l.batch_id"
          + " where b.company_id = :companyId and b.remittance_type in (:types)"
          + " and cast(b.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " and (cast(:insurer as varchar) is null or b.insurer_code = :insurer)"
          + " and (cast(:batchNo as varchar) is null or b.batch_no = :batchNo)"
          + " and not l.excluded order by b.id, l.id";

  private RemittanceSchedules() {}

  private static ReportMetadata metadata(String code, String title, String description) {
    return RemittanceReportSql.metadata(
        code,
        title,
        description,
        true,
        ParameterSpec.optional(BATCH, "Batch No.", ParameterType.TEXT));
  }

  private static ReportResult schedule(
      RemittanceReportSql sql, ReportParameters p, List<String> types, boolean incentives) {
    Map<String, Object> args = RemittanceReportSql.args(p);
    args.put("types", types);
    args.put(
        BATCH, p.optionalText(BATCH).map(String::strip).filter(s -> !s.isEmpty()).orElse(null));
    List<ReportColumn> columns =
        new ArrayList<>(
            List.of(
                ReportColumn.text("invoice_no", "Invoice Number"),
                ReportColumn.text("policy_no", "Policy Number"),
                ReportColumn.text("endorsement_no", "Endorsement Number"),
                ReportColumn.text("assured_name", "Name of Assured"),
                ReportColumn.text("risk_code", "Risk Code"),
                ReportColumn.text("record_status", "Record Status"),
                ReportColumn.date("last_paid_on", "Date Last Paid"),
                ReportColumn.date("inception_date", "Date Inception"),
                ReportColumn.date("booking_date", "Date Booked"),
                ReportColumn.date("expiry_date", "Date Expiry"),
                ReportColumn.amount("paid_ar", "Paid AR"),
                ReportColumn.amount("commission", "Realized Commission"),
                ReportColumn.amount("commission_vat", "Realized VAT"),
                ReportColumn.amount("wtax", "WTAX"),
                ReportColumn.amount("dtip", "DTIP"),
                ReportColumn.amount(
                    "net_due", incentives ? "Net Due Before Incentives" : "Net Due")));
    if (incentives) {
      columns.add(ReportColumn.amount("basic_premium", "Basic Premium"));
      columns.add(ReportColumn.amount("incentive", "Incentive with VAT"));
      columns.add(ReportColumn.amount("net_after_incentive", "Net Due After Incentives"));
    } else {
      columns.add(ReportColumn.date("insurer_or_date", "Date of Original Receipt"));
      columns.add(ReportColumn.text("insurer_or_no", "Official Receipt Number"));
      columns.add(ReportColumn.amount("insurer_or_amount", "OR Amount"));
    }
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy("batch", "Batch No.")
        .rows(sql.rows(SQL, args))
        .presorted()
        .build();
  }

  /** REM-SCHEDULE-NORMAL (Annex III #3). */
  @Component
  public static class NormalSchedule implements ReportDefinition {

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public NormalSchedule(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceSchedules.metadata(
          "REM-SCHEDULE-NORMAL",
          "Remittance Schedule Normal",
          "Accounts of the Normal (peso and dollar) remittance batches (RMTID.011/039)");
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return RemittanceSchedules.schedule(sql, p, List.of("NORMAL_PHP", "NORMAL_USD"), false);
    }
  }

  /** REM-SCHEDULE-SPECIAL (Annex III #4). */
  @Component
  public static class SpecialSchedule implements ReportDefinition {

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public SpecialSchedule(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceSchedules.metadata(
          "REM-SCHEDULE-SPECIAL",
          "Remittance Schedule Special",
          "Accounts of the special remittance batches (MKTID.009, RMTID.039)");
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return RemittanceSchedules.schedule(sql, p, List.of("SPECIAL"), false);
    }
  }

  /** REM-SCHEDULE-INCENTIVE (Annex III #7). */
  @Component
  public static class IncentiveSchedule implements ReportDefinition {

    private final RemittanceReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public IncentiveSchedule(RemittanceReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return RemittanceSchedules.metadata(
          "REM-SCHEDULE-INCENTIVE",
          "Remittance Schedule with Incentives",
          "Accounts of the With Incentives batches with the early remittance incentive (RMTID.023/039)");
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return RemittanceSchedules.schedule(sql, p, List.of("WITH_INCENTIVES"), true);
    }
  }
}
