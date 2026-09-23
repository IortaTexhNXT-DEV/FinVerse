package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ColumnType;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-PC-PENDING – Petty Cash Disbursements Pending Reimbursement (Src FGL018): approved vouchers
 * of each petty cash box not reimbursed as of a date, by expense account, with days pending. The
 * notes give each box's limit, pending disbursements and closing balance (limit − pending).
 */
@Component
public class PettyCashPendingReport implements ReportDefinition {

  private static final String FUND_FROM = "fundFrom";
  private static final String FUND_TO = "fundTo";
  private static final String AMOUNT = "amount";

  private final PettyCashReports reports;

  /**
   * Creates the report.
   *
   * @param reports petty cash read model
   */
  public PettyCashPendingReport(PettyCashReports reports) {
    this.reports = reports;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-PC-PENDING",
        "Petty Cash Disbursements Pending Reimbursement",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Petty cash vouchers not yet reimbursed, per box and expense account (FGL018)",
        List.of(
            GlReportSupport.companyParam(),
            ParameterSpec.optional(FUND_FROM, "Petty Cash Number From", ParameterType.TEXT),
            ParameterSpec.optional(FUND_TO, "Petty Cash Number To", ParameterType.TEXT),
            GlReportSupport.asOfParam()),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate asOf = p.date(GlReportSupport.AS_OF);
    List<Map<String, Object>> source =
        reports.pending(
            p.longValue(GlReportSupport.COMPANY),
            asOf,
            ReportParams.text(p, FUND_FROM),
            ReportParams.text(p, FUND_TO));
    Map<String, BigDecimal[]> boxes = new LinkedHashMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> s : source) {
      rows.add(row(s, asOf));
      BigDecimal[] box =
          boxes.computeIfAbsent(
              (String) s.get("fund_code"),
              k -> new BigDecimal[] {(BigDecimal) s.get("imprest_amount"), BigDecimal.ZERO});
      box[1] = box[1].add((BigDecimal) s.get(AMOUNT));
    }
    TabularReportBuilder builder =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.date("date", "Date"),
                ReportColumn.text("reference", "Reference"),
                ReportColumn.text("documentNo", "Document No"),
                ReportColumn.text("division", "Division"),
                ReportColumn.text("department", "Department"),
                ReportColumn.text("narration", "Narration"),
                ReportColumn.amount(AMOUNT, "Disb. Amount"),
                new ReportColumn("days", "Days Pending", ColumnType.NUMBER, false),
                ReportColumn.text("user", "User ID"),
                ReportColumn.date("entered", "Entry Date"))
            .groupBy("box", "Petty Cash")
            .groupBy("account", "Account")
            .presorted()
            .rows(rows);
    boxes.forEach(
        (code, v) ->
            builder.note(
                code
                    + ": box limit "
                    + v[0].toPlainString()
                    + ", disbursements pending "
                    + v[1].toPlainString()
                    + ", closing balance "
                    + v[0].subtract(v[1]).toPlainString()));
    return builder.build();
  }

  private static Map<String, Object> row(Map<String, Object> s, LocalDate asOf) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(
        "box",
        s.get("fund_code") + " " + s.get("fund_name") + " - Cash A/c " + s.get("gl_account_code"));
    row.put("account", s.get("expense_account_code") + " " + s.get("account_name"));
    row.put("date", s.get("disbursement_date"));
    row.put("reference", s.get("receipt_ref"));
    row.put("documentNo", s.get("document_no"));
    row.put("division", s.get("branch_code"));
    row.put("department", s.get("cost_center"));
    row.put("narration", s.get("description") + " - " + s.get("payee"));
    row.put(AMOUNT, s.get(AMOUNT));
    row.put("days", ChronoUnit.DAYS.between((LocalDate) s.get("disbursement_date"), asOf));
    row.put("user", s.get("created_by"));
    row.put("entered", s.get("created_at"));
    return row;
  }
}
