package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.TaxRemittance;
import com.iortatechnxt.brokerverse.tax.service.TaxReturnService;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * TAX-REMIT – Tax remittance register: returns paid in a date range with period, due date, payment
 * date, days late, amounts cleared and paid, bank, reference and journal.
 */
@Component
public class RemittanceRegisterReport implements ReportDefinition {

  private final TaxReturnService returns;

  /**
   * Creates the report.
   *
   * @param returns returns and remittances
   */
  public RemittanceRegisterReport(TaxReturnService returns) {
    this.returns = returns;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-REMIT",
        "Tax Remittance Register",
        ReportCategory.TAX_STATUTORY,
        "Tax returns paid in a period: due date, payment date, amount and clearing journal",
        List.of(TaxReportSupport.company(), TaxReportSupport.from(), TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    TaxPeriod range = TaxReportSupport.period(p);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (TaxRemittance r :
        returns.remittances(TaxReportSupport.companyId(p), range.from(), range.to())) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("form", r.getFormCode());
      row.put("period", new TaxPeriod(r.getPeriodStart(), r.getPeriodEnd()).label());
      row.put("dueDate", r.getDueDate());
      row.put("paidOn", r.getPaidOn());
      row.put("daysLate", Math.max(0, ChronoUnit.DAYS.between(r.getDueDate(), r.getPaidOn())));
      row.put("cleared", r.getPayableCleared());
      row.put("credit", r.getCreditApplied());
      row.put(TaxReportSupport.AMOUNT, r.getAmount());
      row.put("bank", r.getBankAccountCode());
      row.put("reference", r.getPaymentReference());
      row.put("journal", r.getJournalBatchNo());
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("period", "Period"),
            ReportColumn.date("dueDate", "Due date"),
            ReportColumn.date("paidOn", "Paid on"),
            ReportColumn.text("daysLate", "Days late"),
            ReportColumn.amount("cleared", "Payable cleared"),
            ReportColumn.amount("credit", "Credits applied"),
            ReportColumn.amount(TaxReportSupport.AMOUNT, "Amount paid"),
            ReportColumn.text("bank", "Bank"),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.text("journal", "Journal"))
        .groupBy("form", "Form")
        .rows(rows)
        .build();
  }
}
