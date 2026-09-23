package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.report.core.ColumnType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-PC-REIMB – Petty Cash Reimbursements during Period (Src FGL019): reimbursement documents
 * approved in the period (document date basis) per box, with the vouchers reimbursed and the delay
 * in days between disbursement and reimbursement.
 */
@Component
public class PettyCashReimbursementReport implements ReportDefinition {

  private final PettyCashReports reports;

  /**
   * Creates the report.
   *
   * @param reports petty cash read model
   */
  public PettyCashReimbursementReport(PettyCashReports reports) {
    this.reports = reports;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-PC-REIMB",
        "Petty Cash Reimbursements during Period",
        ReportCategory.RECEIVABLES_PAYABLES,
        "Reimbursements of petty cash boxes in a period with the delay after disbursement (FGL019)",
        List.of(
            GlReportSupport.companyParam(), GlReportSupport.fromParam(), GlReportSupport.toParam()),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> s :
        reports.reimbursed(
            p.longValue(GlReportSupport.COMPANY),
            p.date(GlReportSupport.FROM),
            p.date(GlReportSupport.TO))) {
      LocalDate claimDate = (LocalDate) s.get("claim_date");
      LocalDate paid = (LocalDate) s.get("disbursement_date");
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(
          "box",
          s.get("fund_code")
              + " "
              + s.get("fund_name")
              + " - Cash A/c "
              + s.get("gl_account_code"));
      row.put(
          "claim",
          s.get("claim_no")
              + " dated "
              + claimDate
              + " - Bank "
              + s.get("bank_code")
              + " "
              + s.get("bank_name")
              + " - Journal "
              + s.get("journal_batch_no"));
      row.put("date", paid);
      row.put("reference", s.get("receipt_ref"));
      row.put("documentNo", s.get("document_no"));
      row.put("account", s.get("expense_account_code"));
      row.put("accountName", s.get("account_name"));
      row.put("division", s.get("branch_code"));
      row.put("department", s.get("cost_center"));
      row.put("amount", s.get("amount"));
      row.put("delay", ChronoUnit.DAYS.between(paid, claimDate));
      row.put("user", s.get("created_by"));
      row.put("entered", s.get("created_at"));
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date("date", "Date"),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.text("documentNo", "Voucher No"),
            ReportColumn.text("account", "Main A/c"),
            ReportColumn.text("accountName", "Account Name"),
            ReportColumn.text("division", "Division"),
            ReportColumn.text("department", "Department"),
            ReportColumn.amount("amount", "Reimbursement Amount"),
            new ReportColumn("delay", "Delay (days)", ColumnType.NUMBER, false),
            ReportColumn.text("user", "User Id"),
            ReportColumn.date("entered", "Date"))
        .groupBy("box", "Petty Cash")
        .groupBy("claim", "Reimbursement")
        .presorted()
        .rows(rows)
        .build();
  }
}
