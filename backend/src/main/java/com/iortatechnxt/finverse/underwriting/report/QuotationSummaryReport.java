package com.iortatechnxt.finverse.underwriting.report;

import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.underwriting.domain.Quotation;
import com.iortatechnxt.finverse.underwriting.domain.QuotationIteration;
import com.iortatechnxt.finverse.underwriting.service.QuotationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * QTN-SUMMARY Quotations Converted / Approved / Rejected: count per status (summary, also printed
 * as notes) and the quotations of each status (detail), for quotations issued in the period.
 */
@Component
public class QuotationSummaryReport implements ReportDefinition {

  private static final String STATUS = "status";

  private final QuotationService quotations;

  /**
   * Creates the report.
   *
   * @param quotations quotation service
   */
  public QuotationSummaryReport(QuotationService quotations) {
    this.quotations = quotations;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.addAll(UwReportSupport.dateRange("Period From", "Period To"));
    return new ReportMetadata(
        "QTN-SUMMARY",
        "Quotations Converted / Approved / Rejected",
        ReportCategory.UNDERWRITING,
        "Quotation outcome counts and details by status",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    UwFilters f = UwReportSupport.filters(p);
    Map<String, Integer> counts = new TreeMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Quotation q :
        quotations.search(
            p.longValue(UwReportSupport.COMPANY),
            null,
            p.date(UwReportSupport.FROM),
            p.date(UwReportSupport.TO))) {
      if (f.test(q)) {
        counts.merge(q.getStatus().name(), 1, Integer::sum);
        rows.add(row(q));
      }
    }
    TabularReportBuilder builder =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text(STATUS, "Status"),
                ReportColumn.text("quotationNo", "Quotation"),
                ReportColumn.text("insured", "Insured"),
                ReportColumn.date("issueDate", "Issue Date"),
                ReportColumn.text("validity", "Validity (days)"),
                ReportColumn.amount("si", "Sum Insured"),
                ReportColumn.amount("premium", "Premium"),
                ReportColumn.count("count", "Count"))
            .groupBy(STATUS, "Status")
            .rows(rows);
    counts.forEach((status, count) -> builder.note("Summary - " + status + ": " + count));
    return builder.build();
  }

  private static Map<String, Object> row(Quotation q) {
    QuotationIteration it = q.current();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(STATUS, q.getStatus().name());
    m.put("quotationNo", q.getQuotationNo());
    m.put("insured", q.getInsuredName());
    m.put("issueDate", q.getIssueDate());
    m.put("validity", String.valueOf(q.getValidityDays()));
    m.put("si", it.getSumInsured());
    m.put("premium", it.getGrossPremium());
    m.put("count", 1);
    return m;
  }
}
